package app.opencode.tools

// Port of packages/opencode/src/tool/registry.ts:209-249 (names + permission categories).
// Full descriptions/schemas (.txt + zod) replace these one-liners in Phase 4.

data class ToolDescriptor(val name: String, val description: String, val permission: String)

object ToolRegistry {
    val builtins: List<ToolDescriptor> = listOf(
        ToolDescriptor("read", "Read a file or list a directory", "read"),
        ToolDescriptor("write", "Create or overwrite a file", "edit"),
        ToolDescriptor("edit", "Exact-string file replace", "edit"),
        ToolDescriptor("glob", "Filename glob search", "glob"),
        ToolDescriptor("grep", "Regex content search", "grep"),
        ToolDescriptor("bash", "Run a shell command", "bash"),
        ToolDescriptor("task", "Spawn a subagent", "task"),
        ToolDescriptor("todowrite", "Replace the session todo list", "todowrite"),
        ToolDescriptor("webfetch", "Fetch a URL as text/markdown/html", "webfetch"),
        ToolDescriptor("websearch", "Web search", "websearch"),
        ToolDescriptor("skill", "Load a SKILL.md into context", "skill"),
        ToolDescriptor("question", "Ask the user multiple-choice questions", "question"),
        ToolDescriptor("apply_patch", "Multi-file patch apply (GPT-only alt to edit/write)", "edit"),
        ToolDescriptor("lsp", "Go-to-def/refs/hover/symbols (experimental)", "lsp"),
        ToolDescriptor("plan_exit", "Exit plan mode (plan CLI only)", "plan"),
    )

    fun names(): List<String> = builtins.map { it.name }

    /** Port of permission/index.ts disabled(): deny:* hides the tool from the model. */
    fun visibleTools(denyPermissions: Set<String>): List<ToolDescriptor> =
        builtins.filter { it.permission !in denyPermissions }
}
