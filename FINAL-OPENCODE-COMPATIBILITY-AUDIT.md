# Final OpenCode Compatibility Audit (opencode-1.18.35 vs this port)

## MATCHED

- Agent loop shape: prompt -> context -> model -> tool calls -> validate -> permission -> execute -> results -> repeat (AgentLoop.kt vs prompt.ts runLoop).
- Model-driven tool selection: no keyword routing anywhere; tools exposed with descriptions/schemas, deny hides tool.
- Tool names/permission categories: read/write/edit/glob/grep/bash/task/todowrite/webfetch/websearch/skill/question (+apply_patch/lsp/plan_exit declared).
- Permission model: allow/ask/deny, wildcard last-match-wins, default ask, always-appends-rule, subagent deny inheritance.
- Session/message/part shapes: User/Assistant/ToolCall/ToolResult parts persisted in Room mirroring the sqlite tables.
- Retry policy: max 5, retry-after honored, 2s*2^n cap 30s, 429/5xx, overflow non-retryable.
- Doom-loop guard (3 identical calls -> ask), repair-to-invalid, truncate caps, external-directory gate.
- Subagents: build/plan/general/explore + depth guard + background + task_id resume.
- Git op set + thin-CLI-wrapper philosophy; agent uses bash for git (as original).
- MCP JSON-RPC transports + server.tool namespacing; SKILL.md discovery; webfetch.

## PARTIALLY MATCHED

- Edit: exact-match + ambiguity guard yes; BOM/CRLF preservation, formatter + LSP hooks no.
- Glob/grep: pure-Kotlin matching, same caps; ripgrep binary not vendored.
- Shell: cwd/env/streaming/exit/timeout yes; no PTY, no process-group kill, `sh -c` only.
- Providers: OpenAI-compat (9 endpoints) + Anthropic real; models.dev catalog, OAuth, Azure/Bedrock/Vertex, variants/transforms no.
- Compaction: overflow detection + retry plumbing yes; auto-summarization loop no.
- Prompts: assembly pipeline yes; only default.txt placeholder ships (17 variants + 4 specialists to vendor).
- Todo: replace/list yes; session scoping no (process-wide).

## ANDROID-SPECIFIC

- bun/node-pty/parcel-watcher -> JVM ProcessBuilder + Room; Keystore AES/GCM for keys; prefs for rules/config; SAF pending; Doze limits documented; `sh -i` interactive sessions.
- TUI/electron -> Compose screens (Projects/Chat/Term/Edit/Settings).

## NOT SUPPORTED (documented)

- Signed release APK (no keystore in CI), websearch (needs provider key), MCP OAuth, LSP downloads, custom plugin tools, skills URL auto-pull, session fork/share, parallel tool calls, structured output, per-agent models.

## REQUIRES FUTURE WORK

- Vendor 17 prompt variants verbatim; token accounting + auto-compaction; Dispatchers.IO migration; Room migration past v1; SAF picker; syntax highlighting; credential-helper git auth.
