# Architecture

Kotlin + Compose. Packages mirror `packages/opencode/src`:

- `agent/` — AgentDef/Builtins, AgentLoop (turn loop), PromptBuilder, Subagents (resolve + child rules)
- `model/` — ChatModel/ModelClient, OpenAICompatClient, AnthropicClient, Providers, Retry, SecureKeys
- `tools/` — AgentTool, ToolRegistry (descriptors), ToolRunner (lifecycle), FileTools, BashTool, TaskTool, TodoTool, SkillTool(via skill/), WebfetchTool, Truncate, StubTool honesty placeholders
- `permissions/` — PermissionEngine (wildcard), PermissionSession (ask gate), RuleStore
- `session/` — Session store, Room entities/DAO (SessionDb), RoomSessionStore, Db
- `workspace/` — WorkspaceManager (project roots)
- `terminal/` — CommandExecutor/ProcessCommandExecutor, TerminalSession/ProcTerminalManager
- `git/` — GitClient (+clone with remote redaction)
- `mcp/` — JSON-RPC stdio/HTTP, McpClient/Manager/Tools
- `skill/` — SKILL.md discovery + SkillTool
- `editor/` — LCS Diff
- `ui/` — App nav, Projects/Chat/Term/Edit/Settings, PermissionDialog, Config

Flow: ChatScreen builds client (Keystore key) + executors (workspace root) + TaskTool -> AgentLoop streams events -> ToolRunner validates/asks (dialog)/executes -> results persist in Room + re-enter context.
