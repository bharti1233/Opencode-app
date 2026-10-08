# Phase 10 — Subagents

Port of `tool/task.ts` + `agent/agent.ts` custom agents + `agent/subagent-permissions.ts`.

## What landed

- `agent/Subagents.kt` — `resolveAgent` (built-ins + custom map), `deriveChildRules` (parent DENYs inherited, agent permission entries appended, todowrite/task default-deny).
- `tools/TaskTool.kt` — foreground run in child session (child AgentLoop, child toolset, derived permissions), `background=true` returns `started <id>`, `task_id` resumes (`still running` until done), unknown agent + depth guard errors.
- `ToolRunner.executors(root, task)` — real TaskTool injectable; default stays an honest stub.
- 5 tests (derive rules, explore child really reads a file and sees it in context, unknown agent, background+resume, depth strips nested task).

## Deferred

Per-agent model config (children reuse parent client/model), `task_id` resume across restarts (in-memory map), custom agent file discovery (`.opencode/agent/*.md`), compaction/title/summary hidden agents.
