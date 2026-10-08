# Phase 6 — Filesystem/Workspace

Port of `project/project.ts` (fromDirectory/upsert) workspace half.

## What landed

- `workspace/WorkspaceManager.kt` — base-dir project list/create (name sanitized to `[A-Za-z0-9._-]`, `..` impossible) / open (any dir; outside base flagged `external`, which arms the existing `external_directory` gate in ToolRunner). Tool roots come from here.
- FileTools (Phase 4) already enforce rooting via canonical paths; WorkspaceManager is the single source of roots.
- 2 unit tests (create/list/sanitize, internal vs external open, missing -> null).

## Deferred

SAF external-folder picker (needs UI + DocumentsProvider; external dirs openable by path now), file browsing UI (Phase 14), watcher (disabled per compat — poll fallback later if needed).
