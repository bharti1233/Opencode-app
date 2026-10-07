package app.opencode.agent

// Port of packages/opencode/src/agent/agent.ts (built-ins) + session/prompt.ts loop states.
// Model drives tool selection; this file only declares agents and loop states.

data class AgentDef(
    val name: String,
    val primary: Boolean = false,
    val hidden: Boolean = false,
    val tools: Map<String, Boolean> = emptyMap(), // tool -> enabled
    val permission: Map<String, String> = emptyMap(), // permission -> allow/ask/deny
)

object Builtins {
    val build = AgentDef("build", primary = true)
    val plan = AgentDef("plan", primary = true, permission = mapOf("edit" to "deny"))
    val general = AgentDef("general")
    val explore = AgentDef(
        "explore",
        tools = mapOf(
            "grep" to true, "glob" to true, "read" to true,
            "bash" to true, "webfetch" to true, "websearch" to true,
        ),
    )
    // ponytail: hidden compaction/title/summary agents use tools={} deny-all; added in Phase 10.
    val all: List<AgentDef> = listOf(build, plan, general, explore)
}

enum class LoopState { CONTINUE, STOP, COMPACT }

interface AgentLoop {
    suspend fun prompt(sessionId: String, text: String): LoopState
    fun cancel(sessionId: String)
}
