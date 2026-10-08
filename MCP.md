# MCP

Servers: `{name, command[], cwd, env}` (stdio) or `{name, url, headers}` (HTTP). Transports speak JSON-RPC with id matching; HTTP accepts JSON or SSE-wrapped replies.

Tools surface as `server.tool` with permission `mcp_server_tool`. Manager aggregates servers; all calls route through ToolRunner (permissions/truncation apply).

Not ported: OAuth/client-registration flows (static headers supported), SSE keep-alive re challenging transports. See PHASE-11-MCP.md.
