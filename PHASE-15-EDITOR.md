# Phase 15 — Code Editor / Diff

## What landed

- `editor/Diff.kt` — LCS line diff (Same/Del/Add), 2000-line cap with null fallback.
- `ui/EditorScreen.kt` — project file list (filterable), numbered viewer, full-text edit mode with +N/-M preview and Accept (writes, filesystem-confirmed) / Reject (discard), refresh.
- `ui/App.kt` — Edit screen in project nav.
- 4 diff tests.

## Deferred

Syntax highlighting (no lightweight highlighter vendored yet), agent-edit inline preview inside chat (tool results shown as text now), rename/delete ops (bash covers; UI later).
