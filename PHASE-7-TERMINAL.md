# Phase 7 — Integrated Terminal

Port of `tool/shell.ts` execution half + `core/shell.ts` selection (adapted).

## What landed

- `terminal/ProcessCommandExecutor.kt` — `sh -c` spawn in workspace cwd, env inherited, stdout/stderr split via pump threads, exit codes, timeout kill (124 + `[timed out]` note), bad-dir/spawn-failure as exit -1 errors.
- `terminal/TerminalManager.kt` — interactive `sh` sessions sharing the agent workspace: rolling 100k-char buffer, stdin, cancel; open/list.
- `tools/BashTool.kt` — real `bash` executor (replaces StubTool): command/timeout?/workdir?/description?, non-zero exits annotated `[exit N]`, workdir outside root gated on external_directory.
- 6 unit tests (echo, exit code, timeout kill, bad dir, runner wiring + deny pattern, live interactive session with marker poll).

## Deferred

Command-string file-pattern scan for fine permission patterns (user matches on full command for now), stdin streaming for one-shot runs (ignored, like original), terminal UI screen (Phase 14), long-run foreground-service survival (documented Android limit).
