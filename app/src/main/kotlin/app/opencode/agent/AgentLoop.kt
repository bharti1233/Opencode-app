package app.opencode.agent

import app.opencode.model.ChatMessage
import app.opencode.model.ChatRequest
import app.opencode.model.ModelClient
import app.opencode.model.RequestedToolCall
import app.opencode.model.Retry
import app.opencode.model.StreamEvent
import app.opencode.model.ToolSpec
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionEngine
import app.opencode.permissions.PermissionRequest
import app.opencode.permissions.PermissionSession
import app.opencode.permissions.Verdict
import app.opencode.session.Message
import app.opencode.session.Part
import app.opencode.session.SessionStore
import app.opencode.tools.AgentTool
import app.opencode.tools.ToolRunner
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Port of session/prompt.ts runLoop + processor.ts: model streams text/tool
// calls; tools validate/permission/execute via ToolRunner; results re-enter
// context; repeat until Done without calls, maxSteps, cancel, or fatal error.

sealed interface AgentEvent {
    data class Text(val delta: String) : AgentEvent
    data class ToolStarted(val name: String, val id: String) : AgentEvent
    data class ToolFinished(val name: String, val output: String, val error: String?) : AgentEvent
    data class Completed(val text: String) : AgentEvent
    data class Failed(val message: String) : AgentEvent
}

class AgentLoop(
    private val client: ModelClient,
    private val model: String,
    private val tools: Map<String, AgentTool>,
    private val permissions: PermissionSession,
    private val store: SessionStore,
    private val onPermission: suspend (PermissionRequest) -> Decision,
    private val maxSteps: Int = 32,
) {
    private val jobs = ConcurrentHashMap<String, Job>()

    fun run(sessionId: String, userText: String, system: String): Flow<AgentEvent> = callbackFlow {
        jobs[sessionId] = coroutineContext[Job]!!
        try {
            runInner(sessionId, userText, system)
        } finally {
            jobs.remove(sessionId)
        }
        close()
    }

    fun cancel(sessionId: String) {
        jobs[sessionId]?.cancel()
    }

    private data class Turn(
        val text: String,
        val calls: List<RequestedToolCall>,
        val error: StreamEvent.Error?,
    )

    private suspend fun SendChannel<AgentEvent>.runInner(
        sessionId: String,
        userText: String,
        system: String,
    ) {
        store.append(Message(UUID.randomUUID().toString(), sessionId, "user", listOf(Part.Text(userText))))
        // Port of Permission.disabled: deny:* tools are hidden from the model.
        val visible = tools.filter {
            PermissionEngine.resolve(permissions.rules, it.value.permission, "*") != Verdict.DENY
        }
        val specs = visible.values.map { ToolSpec(it.name, it.description, it.inputSchemaJson) }
        var lastSig = ""
        var repeat = 0
        var retries = 0
        var step = 0
        while (step++ < maxSteps) {
            val history = store.messages(sessionId).flatMap { toChat(it) }
            val req = ChatRequest(model = model, messages = history, tools = specs, system = system)
            val turn = collectTurn(req)
            if (turn.error != null) {
                if (Retry.isOverflow(turn.error.message)) {
                    send(AgentEvent.Failed("context overflow"))
                    return
                }
                if (turn.error.retryable && retries++ < Retry.MAX_RETRIES) {
                    delay(Retry.delayFor(retries, null))
                    step--
                    continue
                }
                send(AgentEvent.Failed(turn.error.message))
                return
            }
            if (turn.calls.isEmpty()) {
                store.append(
                    Message(UUID.randomUUID().toString(), sessionId, "assistant", listOf(Part.Text(turn.text))),
                )
                send(AgentEvent.Completed(turn.text))
                return
            }
            val assistantParts = mutableListOf<Part>(Part.Text(turn.text))
            for (c in turn.calls) {
                // Doom-loop guard (processor.ts): 3 identical calls need explicit approval.
                val sig = "${c.name}:${c.argumentsJson}"
                repeat = if (sig == lastSig) repeat + 1 else 1
                lastSig = sig
                if (repeat >= 3) {
                    if (!permissions.ask("doom_loop", sig, onPermission)) {
                        send(AgentEvent.Failed("possible loop blocked: $sig"))
                        return
                    }
                    repeat = 0
                }
                send(AgentEvent.ToolStarted(c.name, c.id))
                assistantParts += Part.ToolCall(c.id, c.name, "completed", c.argumentsJson)
                val out = ToolRunner.run(c.name, parseArgs(c.argumentsJson), permissions.rules, tools) { p, i ->
                    permissions.ask(p, i, onPermission)
                }
                store.append(
                    Message(
                        UUID.randomUUID().toString(), sessionId, "tool",
                        listOf(Part.ToolResult(c.id, out.output, out.error)),
                    ),
                )
                send(AgentEvent.ToolFinished(c.name, out.output, out.error))
            }
            store.append(Message(UUID.randomUUID().toString(), sessionId, "assistant", assistantParts))
        }
        send(AgentEvent.Failed("max steps ($maxSteps) exceeded"))
    }

    private suspend fun SendChannel<AgentEvent>.collectTurn(req: ChatRequest): Turn {
        val texts = StringBuilder()
        val calls = mutableListOf<RequestedToolCall>()
        var error: StreamEvent.Error? = null
        client.stream(req).collect { e ->
            when (e) {
                is StreamEvent.TextDelta -> {
                    texts.append(e.text)
                    send(AgentEvent.Text(e.text))
                }
                is StreamEvent.ToolCall -> calls += e.call
                is StreamEvent.Done -> {}
                is StreamEvent.Error -> error = e
            }
        }
        return Turn(texts.toString(), calls, error)
    }

    companion object {
        fun toChat(m: Message): List<ChatMessage> = when (m.role) {
            "tool" -> m.parts.filterIsInstance<Part.ToolResult>().map {
                ChatMessage("tool", it.output.ifEmpty { it.error ?: "" }, toolCallId = it.callId)
            }
            "assistant" -> listOf(
                ChatMessage(
                    "assistant",
                    m.parts.filterIsInstance<Part.Text>().joinToString("\n") { it.text },
                    toolCalls = m.parts.filterIsInstance<Part.ToolCall>()
                        .map { RequestedToolCall(it.callId, it.tool, it.args) },
                ),
            )
            else -> listOf(
                ChatMessage("user", m.parts.filterIsInstance<Part.Text>().joinToString("\n") { it.text }),
            )
        }

        fun parseArgs(json: String): Map<String, String> {
            if (json.isBlank()) return emptyMap()
            val o = JSONObject(json)
            return o.keys().asSequence().associateWith { k ->
                if (o.isNull(k)) "" else o.get(k).toString()
            }
        }
    }
}
