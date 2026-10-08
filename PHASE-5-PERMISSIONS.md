# Phase 5 — Permission System

Port of `permission/index.ts` ask/reply + `permission/arity.ts` (arity deferred to shell scan, Phase 7).

## What landed

- `permissions/PermissionEngine.kt` (Phase 0/2) — allow/ask/deny, wildcard last-match-wins, default ask.
- `permissions/PermissionSession.kt` — ALLOW passes / DENY blocks without UI; ASK suspends on `showDialog`; ALLOW_ALWAYS appends an exact-input allow rule (mirrors `always` reply). Mutex-guarded (sibling asks serialize).
- `permissions/RuleStore.kt` — persists `always` choices in private prefs (`permission|pattern|action` lines); loaded at startup, saved after each ask.
- `ui/PermissionDialog.kt` — Deny / Allow / Always; dismiss = deny. Renders engine verdicts only.
- `ui/MainActivity.kt` — real wiring: ask -> dialog -> decision -> persist; result text shows outcome.
- 4 unit tests (allow/deny short-circuit without dialog, approve/reject, always-rule auto-allows second ask).

## Deferred

Bash-prefix arity scan (Phase 7 shell), doom-loop guard (Phase 8), subagent permission derivation (Phase 10).
