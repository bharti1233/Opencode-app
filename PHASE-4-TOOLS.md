# Phase 4 — Tool System

Port of `tool/registry.ts` + `tool/tool.ts` + `session/tools.ts` wrapping + `tool/truncate.ts`.

## What landed

- `tools/FileTools.kt` — ReadTool (file slice w/ offset/limit caps + dir listing + missing-file errors), WriteTool (mkdirs + overwrite), EditTool (exact-match, ambiguous-match guard, replaceAll), GlobTool (glob matcher on rel path + filename, 100 cap), GrepTool (regex, include filter, skips >1MB, `file:line:` output, 100 cap). All rooted at workspace; outside-root detected via canonical paths.
- `tools/ToolRunner.kt` — lifecycle: repair (case-insensitive else `invalid`) -> permission (DENY blocks, ASK suspends on `onAsk`) -> external_directory gate -> required-arg validation (`invalid arguments…rewrite the input`) -> execute -> truncate. `StubTool` for bash/task/todowrite/webfetch/websearch/skill/question: honest phase-owned errors, no fake success.
- `tools/Tool.kt` — AgentTool gains `requiredArgs`, `target(args)`, `isExternal(target)` (default no-ops).
- `tools/Truncate.kt` — 2000 lines / 50k chars caps.
- 9 unit tests (roundtrips, dir list, edit miss, glob/grep, deny-blocks-without-side-effects, ask approve/reject, repair, external gate, truncate caps).

## Deferred

ripgrep binary (pure-Kotlin matching for now), BOM/CRLF preservation + format/LSP hooks on edit (Phase 6/12), bash (Phase 7), task (Phase 10), todowrite (Phase 9), web/skill (Phase 11), question (Phase 8), MCP/custom tools (Phase 11).
