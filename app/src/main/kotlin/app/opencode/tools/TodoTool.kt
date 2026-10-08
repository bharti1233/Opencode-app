package app.opencode.tools

import org.json.JSONArray

// Port of tool/todo.ts: replace the todo list. Deviation: process-wide list
// (original is session-scoped); session scoping deferred with Phase 9's
// token-accounting follow-up.

data class Todo(val content: String, val status: String)

class TodoTool : AgentTool {
    override val name = "todowrite"
    override val description = "Replace the session todo list"
    override val inputSchemaJson =
        """{"type":"object","properties":{"todos":{"type":"array","items":{"type":"object"}}}},"required":["todos"]}"""
    override val permission = "todowrite"
    override val requiredArgs = listOf("todos")

    private val items = mutableListOf<Todo>()
    fun list(): List<Todo> = items.toList()

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val arr = try {
            JSONArray(args.getValue("todos"))
        } catch (_: Exception) {
            return ToolResult("", error = "todos must be a JSON array")
        }
        items.clear()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            items += Todo(o.optString("content", ""), o.optString("status", "pending"))
        }
        return ToolResult(
            items.mapIndexed { i, t -> "${i + 1}. [${t.status}] ${t.content}" }.joinToString("\n")
                .ifEmpty { "empty" },
        )
    }
}
