# Phase 8 — Agent Loop

Port of `session/prompt.ts` runLoop + `processor.ts` (stream handling, doom-loop guard, retry).

## What landed

- `agent/AgentLoop.kt` — user msg -> history -> model stream -> text deltas + tool calls -> ToolRunner (permission via PermissionSession) -> tool results appended -> repeat. Done-without-calls completes; deny:* tools hidden from model; 3 identical calls trigger `doom_loop` ask; retryable errors retry (max 5, backoff); overflow fatal; maxSteps guard; per-session Job cancel.
- `agent/PromptBuilder.kt` — system assembly (base + env + instructions).
- `session/Session.kt` — Part gains `ToolCall.args` + `ToolResult(callId, output, error)`; `toChat` maps store <-> model messages both ways.
- 6 loop tests with scripted FakeClient, zero network (text turn, real-read roundtrip with context assertion, doom guard call-count, retry, deny-hides-tools, arg parsing).

## Deferred

Parallel tool calls (sequential now), compaction trigger inside loop (Phase 9), subagent task routing (Phase 10), streaming UI consumption (Phase 14), structured-output mode.
