# Agents

Built-ins (from `agent.ts`): `build` (primary, full), `plan` (primary, edit-deny), `general` (subagent), `explore` (read-only allow-list). Hidden compaction/title/summary agents not yet wired.

Selection is model-driven: system + agent instructions + project context + conversation + tool definitions + permissions -> model -> tool calls. No intent routing in code.

Subagents run child AgentLoops in child sessions with derived permissions (parent DENYs inherited, todowrite/task default-deny), depth-guarded, foreground or background with task_id resume. See PHASE-10-SUBAGENTS.md.
