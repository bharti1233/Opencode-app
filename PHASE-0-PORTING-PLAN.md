# Phase 0 — Porting Plan

## KEEP (conceptually unchanged)

Tool logic + schemas (read/write/edit/glob/grep/apply_patch/task/todo/webfetch/websearch/skill/question/plan), permission allow/ask/deny + wildcard last-match-wins, agent loop + model-driven selection, session/message/part data shapes, compaction/summary/title prompts, provider catalog idea, MCP/skill extension shape, git op set.

## PORT (logic -> Kotlin)

`agent/` loop+reminders, `session/` messages/parts/processor/prompt-builder/compaction/retry, `tools/` registry+validation+truncate, `permissions/` engine+arity, `model/` catalog+selection+streaming+tool-call mapping, `config/` deep-merge, `prompts/` all 18 base + 4 specialist texts (verbatim assets), `storage/` Room entities mirroring tables.

## ADAPT (architecture same, impl changes)

Shell: ProcessBuilder, cwd=workspace, env merge, stdout/stderr streaming, timeout+kill (no -pid), `$PREFIX/bin/bash`->sh. FS: workspace-bounded + SAF. Ripgrep: system/vendor binary w/ Kotlin fallback. Watcher: disabled/poll. DB: Room (drizzle schema port). Events: StateFlow/SharedFlow + SSE client. Server: drop embedded HTTP (or Ktor stub, OPTIONAL). Auth: EncryptedSharedPreferences/Android Keystore.

## REPLACE

bun -> JVM; node-pty -> ProcessBuilder + terminal emulator (TerminalSession); parcel/watcher -> no-op/poll; glibc LSP downloads -> Termux pkgs or off; TUI/opentui + electron desktop -> Jetpack Compose; install script combo -> Gradle APK + GitHub Actions.

## OPTIONAL (phase 1 scope-out)

`execute` code-mode, `lsp` tool, `plan_exit`, desktop, storybook, slack/console/stats/enterprise, e2e Playwright, format/* (no-op), most LSP servers.

## BLOCKER

None hard (all fail-soft) except must solve: (1) ripgrep binary on android, (2) Bun-only boot paths, (3) process-group kill. All addressed in ADAPT.

## Order

0 audit (done) -> 1 agent-behavior docs -> 2 Kotlin core interfaces (this repo skeleton) -> 3 model/provider -> 4 tools -> 5 permissions -> 6 FS/workspace -> 7 terminal -> 8 loop -> 9 session/context -> 10 subagents -> 11 MCP/skills -> 12 git -> 13 github -> 14 UI -> 15 editor/diff -> 16 build/CI -> 17 feedback loop -> 18 APK verify -> 19 compat audit -> 20 review -> 21 final APK -> 22 docs.
