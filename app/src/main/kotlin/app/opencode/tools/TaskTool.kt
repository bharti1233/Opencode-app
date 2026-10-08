package app.opencode.tools

import app.opencode.agent.AgentDef
import app.opencode.agent.AgentEvent
import app.opencode.agent.AgentLoop
import app.opencode.agent.deriveChildRules
import app.opencode.model.ModelClient
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionRequest
import app.opencode.permissions.PermissionSession
import app.opencode.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import java.io.File

// Port of tool/task.ts: spawn a subagent in a child session (foreground or
// background), optionally resuming a finished background task by task_id.
// Per-agent model config is deferred (same client/model as parent for now).

class TaskTool(
    private val defs: Map<String, AgentDef>,
    private val client: ModelClient,
    private val model: String,
    private val root: File,
    private val store: SessionStore,
    private val scope: CoroutineScope,
    private val parentRules: List<app.opencode.permissions.Rule> = emptyList(),
    private val onPermission: suspend (PermissionRequest) -> Decision = { Decision.DENY },
    private val depth: Int = 1,
    private val tasks: MutableMap<String, Deferred<String>> = mutableMapOf(),
) : AgentTool {
    override val name = "task"
    override val description = "Spawn a subagent (foreground or background)"
    override val inputSchemaJson =
        """{"type":"object","properties":{"description":{"type":"string"},"prompt":{"type":"string"},"subagent_type":{"type":"string"},"task_id":{"type":"string"},"background":{"type":"boolean"}},"required":["description","prompt","subagent_type"]}"""
    override val permission = "task"
    override val requiredArgs = listOf("description", "prompt", "subagent_type")
    override fun target(args: Map<String, String>) = args["subagent_type"]
    private var n = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun execute(args: Map<String, String>): ToolResult {
        args["task_id"]?.let { id ->
            val d = tasks[id] ?: return ToolResult("", error = "unknown task: $id")
            if (!d.isCompleted) return ToolResult("still running: $id")
            return ToolResult(runCatching { d.getCompleted() }.getOrElse { "task failed: ${it.message}" })
        }
        val def = defs[args["subagent_type"]] ?: return ToolResult("", error = "unknown agent: ${args["subagent_type"]}")
        if (depth <= 0) return ToolResult("", error = "max subagent depth exceeded")
        if (args["background"] == "true") {
            val id = "task_${++n}"
            tasks[id] = scope.async { runChild(def, args.getValue("prompt")) }
            return ToolResult("started $id")
        }
        return ToolResult(runChild(def, args.getValue("prompt")))
    }

    private suspend fun runChild(def: AgentDef, prompt: String): String {
        val childSession = store.create(root.path)
        var final = ""
        AgentLoop(
            client = client,
            model = model,
            tools = childTools(def),
            permissions = PermissionSession(deriveChildRules(parentRules, def).toMutableList()),
            store = store,
            onPermission = onPermission,
        ).run(childSession.id, prompt, "You are the ${def.name} subagent.").collect { e ->
            if (e is AgentEvent.Completed) final = e.text
            if (e is AgentEvent.Failed) final = "subagent failed: ${e.message}"
        }
        return final
    }

    /** Child toolset: depth-1 task nesting, filtered to the agent's allow-list when set. */
    fun childTools(def: AgentDef): Map<String, AgentTool> {
        val base = ToolRunner.executors(root).toMutableMap()
        if (depth - 1 <= 0) base.remove("task")
        else base["task"] = childTaskTool()
        if (def.tools.isNotEmpty()) return base.filterKeys { def.tools[it] == true }
        return base
    }

    private fun childTaskTool(): TaskTool = TaskTool(
        defs, client, model, root, store, scope, parentRules, onPermission, depth - 1, tasks,
    )
}
