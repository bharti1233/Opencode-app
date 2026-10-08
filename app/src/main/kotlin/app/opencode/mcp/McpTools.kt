package app.opencode.mcp

import app.opencode.tools.AgentTool
import app.opencode.tools.ToolResult

// MCP tools enter the model as namespaced server.tool entries with their own
// permission key (port of session/tools.ts MCP wiring).

class McpTool(
    private val client: McpClient,
    private val def: McpToolDef,
) : AgentTool {
    override val name = "${client.server.name}.${def.name}"
    override val description = def.description
    override val inputSchemaJson = def.inputSchemaJson
    override val permission = "mcp_${client.server.name}_${def.name}"
    override fun target(args: Map<String, String>) = null

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val o = org.json.JSONObject()
        args.forEach { (k, v) -> o.put(k, v) }
        return try {
            ToolResult(client.callTool(def.name, o.toString()))
        } catch (e: Exception) {
            ToolResult("", error = e.message ?: "mcp call failed")
        }
    }
}

class McpManager {
    private val clients = mutableListOf<McpClient>()

    fun add(client: McpClient): McpManager {
        clients += client
        return this
    }

    fun tools(): Map<String, AgentTool> =
        clients.flatMap { c ->
            c.listTools().map { def ->
                val t = McpTool(c, def)
                t.name to t
            }
        }.toMap()

    fun close() = clients.forEach { runCatching { it.close() } }
}
