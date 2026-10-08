# Phase 12 — Git

Port of `git/index.ts` (thin CLI wrapper, errors surfaced).

## What landed

- `git/GitClient.kt` — status/diff/log/add/commit/branch/checkout/stash/pull/push/isRepo over the shared CommandExecutor (60s timeout, `--no-optional-locks`); failures throw GitError with real stderr.
- Fidelity note: no dedicated git agent tools — like the original, the agent drives git through bash. GitClient serves UI/workspace layers.
- 3 tests on real temp repos (status/commit/log, diff/branch, non-repo errors).

## Deferred

Auth UX for push/pull (credential helper wiring, Phase 13/14), merge/stash-apply extras, diff stats caps (10 MB cap in original `vcs.ts` — outputs pass through Truncate instead).
