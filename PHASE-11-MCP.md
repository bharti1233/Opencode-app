# Phase 11 — MCP / Skills / Extensibility

Port of `mcp/index.ts` (transports), `skill/discovery.ts` + `tool/skill.ts`, `tool/webfetch.ts`.

## What landed

- `mcp/Mcp.kt` — JSON-RPC framing, `StdioTransport` (spawn + initialize handshake + id-matched line reads + timeout), `HttpTransport` (JSON or SSE-wrapped replies), `McpClient.listTools/callTool` (text join, isError -> throw).
- `mcp/McpTools.kt` — `McpManager` aggregates servers; each tool exposed as `server.tool` with `mcp_server_tool` permission key.
- `skill/Skills.kt` — SKILL.md frontmatter discovery + real `SkillTool` (returns body; unknown -> error).
- `tools/WebfetchTool.kt` — real GET with UA + 200k cap. Wired into default executors (as is SkillTool, rooted at `.opencode/skills`).
- 5 tests (frontmatter, skill load/miss, JSON-RPC shape, stdio echo roundtrip via `cat`, localhost fetch incl. failure).

## Deferred

MCP OAuth/client-registration (explicit per-server headers supported now), remote SSE keep-alive re challenging transports, `websearch` (needs provider key — honest stub), custom plugin tools (`tool/tools/*`, `plugin.tool`), skills auto-pull from URLs.
