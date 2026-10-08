# Phase 14 — Native Android UI

Jetpack Compose, mobile-first dark Material.

## What landed

- `ui/App.kt` — Projects / Chat / Term / Settings nav (project-scoped Chat+Term).
- `ui/ProjectsScreen.kt` — list + create (name sanitized by WorkspaceManager) + open.
- `ui/ChatScreen.kt` — real agent wiring: Room session per project dir, provider client from Keystore-held key, full ToolRunner executors incl. TaskTool, PromptBuilder system from assets, streaming AgentEvents into message list, ASK verdicts through PermissionDialog with persistence.
- `ui/TerminalScreen.kt` — live interactive session on the same project dir (500ms poll), stdin send.
- `ui/SettingsScreen.kt` — provider/model config + API key into Keystore.
- `ui/Config.kt`, `session/Db.kt` (allowMainThreadQueries documented as v0.1 pragmatism).

## Deferred

Diff/Git/Build/Logs/Models/Tools/MCP screens (Phase 15-16 surface existing layers), syntax highlighting (Phase 15), multi-session picker (one session per project dir now), Dispatchers.IO migration.
