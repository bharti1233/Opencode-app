package app.opencode.tools

// Port of packages/opencode/src/tool/tool.ts + registry.ts (descriptors only; exec in Phase 4).

data class ToolResult(val output: String, val truncated: Boolean = false, val error: String? = null)

interface AgentTool {
    val name: String
    val description: String
    val inputSchemaJson: String
    val permission: String
    suspend fun execute(args: Map<String, String>): ToolResult
}
