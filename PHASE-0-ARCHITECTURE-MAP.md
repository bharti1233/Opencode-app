# Phase 0 — Architecture Map

```
User request
  -> session/prompt.ts:prompt() (revert.cleanup, createUserMessage, touch, loop)
  -> loop() / runLoop() while(true)
     -> MessageV2.latest (lastUser/lastAssistant/tasks)
     -> exit check (assistant.finish not tool-calls/unknown)
     -> subtask -> handleSubtask | compaction task -> compaction.process
     -> overflow? -> compaction.create(auto)
     -> agents.get(agent) + SessionReminders.apply + fresh Assistant msg + processor.create
     -> SessionTools.resolve (registry + MCP tools, permission-aware)
     -> system [env, instructions, mcp, skills] + toModelMessagesEffect
     -> llm.stream (ai@6 streamText, timeoutFetch + SSE wrap, Effect.retry policy max 5)
     -> events -> DB writes (Message/Part rows)
     -> stop | compact | continue
```

## Layers

- Transport: `server/` Effect HttpApi + Node HTTP (port 4096, CORS, mDNS, `/doc` OpenAPI), `GET /event` SSE + PTY WebSocket.
- Events: `bus/global.ts` EventEmitter in-process; `event-v2-bridge.ts` per-directory filter; `GET /event` fans out + 10s heartbeat.
- Session: `session/session.ts` CRUD over drizzle/bun:sqlite (`Account/Project/Session/Message/Part/Share/Workspace`); legacy JSON KV in `storage/storage.ts`.
- Messages: `User {id sessionID role agent model format system tools summary}` / `Assistant {…parentID modelID providerID mode path cost tokens time finish error summary}` / `Part` union (text/reasoning/file/agent/compaction/subtask/retry/step-start/step-finish/tool/patch/snapshot); ToolState pending/running/completed/error.
- Providers: `provider/provider.ts` models.dev catalog -> internal Model; bundled AI-SDK factories (~20 providers); auth per-provider env/OAuth/AWS/GCP; variants via ProviderTransform; priority sort; `parseModel("provider/model")`.
- Config: `core/src/v1/config/config.ts` deep-merge (global + legacy TOML + env flags + per-dir `.opencode/` + remote well-known); keys: model/small_model/agent/subagent_depth/providers/mcp/skills/permissions/tools/formatter/lsp/instructions/compaction/tool_output/experimental.
- Extensions: MCP SDK client (stdio/SSE/streamable, OAuth, `server.tool` namespace); skills via `SKILL.md` frontmatter scan + `skills.paths/urls` pull; custom tools `tool/tools/*.{js,ts}` + `plugin.tool`.
- Git: thin CLI wrapper (`git/index.ts`), `project/vcs.ts` status/diff/apply + branch watcher -> BranchUpdated.
