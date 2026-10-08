# Final Report — OpenCode Android Port

- Original: opencode-1.18.35 (bun monorepo, 7403 files), audited in /tmp; 8 Phase-0 + 5 Phase-1 docs in repo.
- Ported: native Kotlin + Compose, Room, OkHttp-SSE, coroutines; Room schema v1.

## Implemented

Agent loop (turns, streaming events, retries, doom guard, deny-hides-tools, cancel); model clients (OpenAI-compat + Anthropic, Providers factory, SecureKeys/Keystore); tools (read/write/edit/glob/grep/bash/task/todowrite/webfetch/skill/MCP, repair/permission/truncate lifecycle); permissions (engine/session/persistence/dialog); workspace; terminal (one-shot + interactive); Room sessions; subagents; git client + clone; UI (Projects/Chat/Term/Edit/Settings); editor diff.

## Compatibility

See FINAL-OPENCODE-COMPATIBILITY-AUDIT.md: core loop/selection/permissions/sessions/retries matched; file/shell/providers partial with documented deviations; Android-specific replacements listed; unsupported items (signed release, websearch key, MCP OAuth, LSP downloads, plugin tools, auto-compaction) explicit with owners.

## Tests

64 JVM unit tests (all green in CI): permissions, retry, parsers, tool roundtrips + gates, processes, git, Room mapping, subagents, MCP framing, diff. Device-thin code covered via pure cores.

## CI

`android` workflow: testDebugUnitTest + assembleDebug + APK artifact, green. Debug APK verified: valid zip, 14 dex files, prompts asset, ~9.6 MB. Release signing not wired (BUILD.md).

## Remaining

Vendor 17 prompt variants; token accounting + auto-compaction; Dispatchers.IO; Room migration v2+; SAF picker; highlighting; git credential UX; parallel calls; per-agent models; session-share/fork; foreground-service survival.
