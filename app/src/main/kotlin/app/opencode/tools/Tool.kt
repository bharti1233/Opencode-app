package app.opencode.tools

// Port of packages/opencode/src/tool/tool.ts + registry.ts (descriptors only; exec in Phase 4).

data class ToolResult(val output: String, val truncated: Boolean = false, val error: String? = null)

interface AgentTool {
    val name: String
    val description: String
    val inputSchemaJson: String
    val permission: String
    val requiredArgs: List<String> get() = emptyList()
    suspend fun execute(args: Map<String, String>): ToolResult
    /** Primary target (path/pattern/command) for permission matching; null = no target. */
    fun target(args: Map<String, String>): String? = null
    /** True when target escapes the allowed workspace (needs external_directory). */
    fun isExternal(target: String): Boolean = false
}
