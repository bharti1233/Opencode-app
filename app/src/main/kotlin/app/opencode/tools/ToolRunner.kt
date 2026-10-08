package app.opencode.tools

import app.opencode.permissions.PermissionEngine
import app.opencode.permissions.Rule
import app.opencode.permissions.Verdict

// Port of session/tools.ts wrapping + tool/tool.ts validation + llm.ts repairToolCall.
// Lifecycle: repair name -> permission -> external-dir -> validate -> execute -> truncate.
// ASK verdicts suspend on onAsk (wired to the UI dialog in Phase 5/8).

/** Honest placeholder: reports its owning phase instead of faking success. */
class StubTool(
    private val id: String,
    private val why: String,
    override val permission: String = id,
) : AgentTool {
    override val name = id
    override val description = "Not yet implemented ($why)"
    override val inputSchemaJson = "{\"type\":\"object\",\"properties\":{}}"
    override suspend fun execute(args: Map<String, String>) =
        ToolResult("", error = "$id not yet implemented ($why)")
}

object ToolRunner {
    /** Port of experimental_repairToolCall: case-insensitive match, else invalid. */
    fun repair(name: String, known: Set<String>): String {
        if (name in known) return name
        val lower = name.lowercase()
        if (lower in known) return lower
        return "invalid"
    }

    suspend fun run(
        name: String,
        args: Map<String, String>,
        rules: List<Rule>,
        executors: Map<String, AgentTool>,
        onAsk: suspend (permission: String, input: String) -> Boolean = { _, _ -> false },
    ): ToolResult {
        val id = repair(name, executors.keys)
        if (id == "invalid") return ToolResult("", error = "unknown tool: $name")
        val tool = executors[id]!!
        val input = tool.target(args) ?: "*"
        when (PermissionEngine.resolve(rules, tool.permission, input)) {
            Verdict.DENY -> return ToolResult("", error = "permission denied: ${tool.permission}")
            Verdict.ASK -> if (!onAsk(tool.permission, input)) {
                return ToolResult("", error = "permission rejected: ${tool.permission}")
            }
            Verdict.ALLOW -> {}
        }
        tool.target(args)?.let { t ->
            if (tool.isExternal(t) &&
                PermissionEngine.resolve(rules, "external_directory", t) != Verdict.ALLOW
            ) {
                return ToolResult("", error = "external directory needs permission: $t")
            }
        }
        val missing = tool.requiredArgs.filter { it !in args }
        if (missing.isNotEmpty()) {
            return ToolResult("", error = "invalid arguments, missing ${missing.joinToString()}; rewrite the input")
        }
        val out = try {
            tool.execute(args)
        } catch (e: Exception) {
            return ToolResult("", error = e.message ?: "tool failed")
        }
        if (out.error != null) return out
        val (text, truncated) = Truncate.apply(out.output)
        return out.copy(output = text, truncated = truncated)
    }

    /** Executors for one workspace root. bash->Phase 7, task->Phase 10, web/skill->Phase 11. */
    fun executors(root: java.io.File): Map<String, AgentTool> = mapOf(
        "read" to ReadTool(root),
        "write" to WriteTool(root),
        "edit" to EditTool(root),
        "glob" to GlobTool(root),
        "grep" to GrepTool(root),
        "bash" to StubTool("bash", "Phase 7 terminal"),
        "task" to StubTool("task", "Phase 10 subagents"),
        "todowrite" to StubTool("todowrite", "Phase 9 sessions"),
        "webfetch" to StubTool("webfetch", "Phase 11 extensions"),
        "websearch" to StubTool("websearch", "Phase 11 extensions"),
        "skill" to StubTool("skill", "Phase 11 extensions"),
        "question" to StubTool("question", "Phase 8 loop"),
    )
}
