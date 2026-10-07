# Phase 1 — Permission Model

Actions allow|ask|deny; Rule{permission,pattern,action}; last-match-wins over merged agent+session rulesets, default ask (`permission/index.ts:28-38`, `schema/v1/permission.ts`). Wildcard: `*`->`.*`, `?`->`.`, trailing ` *` optional, slash-normalized (`core/util/wildcard.ts`).

`Permission.ask`: all-allow -> return; any-deny -> DeniedError; else pending Request + `permission.asked` event, block on Deferred; reply once/always (appends allow rule)/reject (RejectedError, cancels siblings). `~/`/`$HOME` expanded. `disabled()/visibleTools()` hides deny:* tools. Subagents inherit parent denies + external_directory; todowrite/task default-deny (`subagent-permissions.ts`).

Android: `permissions/PermissionEngine.kt` (same match/resolve), UI dialog Deny/Allow(+always), persisted rules; shell pre-scan + external-directory assert preserved.
