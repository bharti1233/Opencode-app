# Phase 1 — Agent Architecture

Loop (`session/prompt.ts:runLoop`, `processor.ts:process`): prompt -> createUserMessage -> loop{ latest -> exit? -> subtask/compaction? -> overflow? -> resolve agent -> reminders -> fresh Assistant -> SessionTools.resolve -> system[env,instructions,mcp,skills] + model messages -> llm.stream (retry max 5) -> events/DB -> stop|compact|continue }.

Agents (`agent/agent.ts:140`): primary build (full), plan (edit-deny), subagent general/explore (read-only subset), hidden compaction/title/summary (no tools). User cfg.agent merged. Subagent via `task` tool -> child session (permission derived from parent) -> recursive prompt -> result injected; background supported; `task_id` resume.

Android: `agent/Agent.kt` (AgentDef, Builtins build/plan/general/explore + hidden), `AgentLoop.kt` (same states). Model decides; Kotlin validates/enforces/executes.
