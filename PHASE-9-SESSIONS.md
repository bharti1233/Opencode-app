# Phase 9 — Session/Context Persistence

Port of `storage/schema.ts` tables + `session/session.ts` CRUD (persistence half).

## What landed

- `session/SessionDb.kt` — Room entities (sessions/messages/parts), blocking DAO with transactional append, `SessionTables` pure mapping (kind-tagged parts incl. args + tool results/errors).
- `session/RoomSessionStore.kt` — SessionStore over Room: create/get/list/messages/append/rename; append touches updatedAt. Sessions survive app close/reopen.
- 2 unit tests (table roundtrip incl. args, error survival). DB I/O itself needs a device (no Robolectric); mapping is the logic, tested.

## Deferred

Compaction/pruning inside the loop (needs token accounting from real model calls), session fork/share, Room migration strategy past v1, moving callers to Dispatchers.IO (Phase 14).
