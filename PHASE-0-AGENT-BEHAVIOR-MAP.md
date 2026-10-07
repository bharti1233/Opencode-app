# Phase 0 — Agent Behavior Map

Mechanism (no keyword routing anywhere):

```
system instructions + agent instructions + project context + conversation
+ tool definitions/descriptions/schemas + permissions + model caps
-> MODEL -> tool selection -> execution -> tool result -> MODEL -> repeat
```

- `SessionTools.resolve` (tools.ts:41) builds `Record<string,AITool>` from `registry.tools({model,agent,session.permission})` + MCP resource/tools; wrapped with `tool.execute.before/after` + `ctx.ask`.
- `llm/request.ts:resolveTools:210`: `Permission.disabled(keys, merge(agent.permission, session.permission))` + `user.tools[k]!==false`; sorted keys; `activeTools + toolChoice` -> `streamText` (llm.ts:317). `experimental_repairToolCall` lowercases or routes to `invalid` (llm.ts:296).
- Built-in agents (`agent/agent.ts:140`): primary `build` (full+question/plan_enter), `plan` (edit deny), subagent `general` (research/parallel), `explore` (grep/glob/list/bash/webfetch/websearch/read only), hidden `compaction/title/summary` (all-tools deny); user `cfg.agent` merged.
- `task` tool: depth check (`subagent_depth` default 1), `ctx.ask(task)`, `sessions.create({parentID})` with `deriveSubagentSessionPermission` (parent denies + external_directory inherited; todowrite/task default-deny), recursive `ops.prompt` on child; `task_id` resume; `background` returns immediately + injects later.
- Prompts: base selected per model in `session/system.ts:provider():28` (default/anthropic/beast/gpt/gpt-astra/codex/gemini/kimi/meta/trinity/copilot-gpt-5) + `system.ts:environment` dynamic env + skills/mcp appendices + `instruction.ts:system` (AGENTS.md/CLAUDE.md + config instructions) + reminders (plan-mode etc). `agent/generate.txt` synthesizes agent JSON; `explore/compaction/summary/title.txt` for specialists.
- Compaction: `overflow.ts` (tokens.total >= usable(context-reserved)); `compaction.create/process` (tail by token budget, preserve_recent_tokens 25%, tail_turns; serialize head w/ 2k trunc; compaction agent with tools:{}; tail_start_id update; overflowed user msg replay or synthetic Continue); opt-in `prune` (40k protect/20k min).
- Retry: `retry.ts` on 429/5xx/rate-limit/overloaded/network/timeout; `retry-after(-ms)` else `2s*2^(n-1)+jitter`, cap 30s; max 5; `ContextOverflowError` -> compaction path, else assistant.error + Error event + idle.
- Permission reject -> `blocked=shouldBreak` unless `continue_loop_on_deny`; abort -> AbortedError + finalizeInterruptedAssistant; content-filter/structured-miss -> typed break.
