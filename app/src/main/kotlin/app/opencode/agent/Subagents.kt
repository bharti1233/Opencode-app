package app.opencode.agent

import app.opencode.permissions.Action
import app.opencode.permissions.Rule

// Port of agent/agent.ts built-ins lookup + agent/subagent-permissions.ts:
// children inherit parent DENYs; todowrite/task default-deny unless the
// subagent opts in; agent-level permission entries append.

fun resolveAgent(name: String, custom: Map<String, AgentDef> = emptyMap()): AgentDef? =
    custom[name] ?: Builtins.all.find { it.name == name }

fun deriveChildRules(parent: List<Rule>, def: AgentDef): List<Rule> {
    val out = parent.filter { it.action == Action.DENY }.toMutableList()
    def.permission.forEach { (perm, act) ->
        out += Rule(perm, "*", Action.valueOf(act.uppercase()))
    }
    if (def.tools["todowrite"] != true) out += Rule("todowrite", "*", Action.DENY)
    if (def.tools["task"] != true) out += Rule("task", "*", Action.DENY)
    return out
}
