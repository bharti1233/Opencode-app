package app.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.opencode.agent.AgentEvent
import app.opencode.agent.AgentLoop
import app.opencode.agent.Builtins
import app.opencode.agent.PromptBuilder
import app.opencode.model.Providers
import app.opencode.model.SecureKeys
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionRequest
import app.opencode.permissions.PermissionSession
import app.opencode.permissions.RuleStore
import app.opencode.session.RoomSessionStore
import app.opencode.session.openDb
import app.opencode.tools.TaskTool
import app.opencode.tools.ToolRunner
import app.opencode.workspace.Project
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class UiMsg(val role: String, val text: String)

@Composable
fun ChatScreen(project: Project) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { RuleStore(ctx) }
    val perms = remember { PermissionSession(store.load().toMutableList()) }
    val db = remember { openDb(ctx) }
    val sessions = remember { RoomSessionStore(db.dao()) }
    val cfg = remember { loadConfig(ctx) }
    var pending by remember {
        mutableStateOf<Pair<PermissionRequest, CompletableDeferred<Decision>>?>(null)
    }
    var msgs by remember { mutableStateOf(listOf(UiMsg("system", "project: ${project.dir.path}"))) }
    var input by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    suspend fun askUi(req: PermissionRequest): Decision {
        val d = CompletableDeferred<Decision>()
        pending = req to d
        val dec = d.await()
        pending = null
        store.save(perms.rules)
        return dec
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || running) return
        input = ""
        msgs = msgs + UiMsg("user", text)
        running = true
        scope.launch {
            try {
                val keys = SecureKeys(ctx)
                val client = Providers.create(cfg.provider) { keys.get(cfg.provider) }
                val session = sessions.list().find { it.directory == project.dir.path }
                    ?: sessions.create(project.dir.path)
                val task = TaskTool(
                    Builtins.all.associateBy { it.name }, client, cfg.model,
                    project.dir, sessions, this, perms.rules, ::askUi,
                )
                val tools = ToolRunner.executors(project.dir, task)
                val base = try {
                    ctx.assets.open("prompts/default.txt").bufferedReader().readText()
                } catch (_: Exception) {
                    "You are a coding agent."
                }
                AgentLoop(client, cfg.model, tools, perms, sessions, ::askUi)
                    .run(session.id, text, PromptBuilder.build(base, cfg.model))
                    .collect { e ->
                        msgs = when (e) {
                            is AgentEvent.Text -> {
                                val last = msgs.lastOrNull()
                                if (last != null && last.role == "assistant") {
                                    msgs.dropLast(1) + last.copy(text = last.text + e.delta)
                                } else msgs + UiMsg("assistant", e.delta)
                            }
                            is AgentEvent.ToolStarted -> msgs + UiMsg("tool", "run: ${e.name}")
                            is AgentEvent.ToolFinished ->
                                msgs + UiMsg("tool", (e.output.ifEmpty { e.error ?: "" }).take(2000))
                            is AgentEvent.Completed -> msgs
                            is AgentEvent.Failed -> msgs + UiMsg("error", e.message)
                        }
                    }
                // history already persisted by the loop via store
            } catch (e: Exception) {
                msgs = msgs + UiMsg("error", e.message ?: "failed")
            }
            running = false
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Chat — ${project.name}")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            msgs.forEach { m -> Text("[${m.role}] ${m.text.take(2000)}") }
        }
        Row(Modifier.fillMaxWidth()) {
            TextField(input, { input = it }, Modifier.weight(1f), label = { Text("task") })
            Button(onClick = { send() }, enabled = !running) { Text("Send") }
        }
    }
    pending?.let { (req, deferred) ->
        PermissionDialog(req) { deferred.complete(it) }
    }
}
