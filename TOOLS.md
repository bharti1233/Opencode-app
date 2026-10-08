# Tools

| tool | status | notes |
|---|---|---|
| read/write/edit/glob/grep | real | workspace-rooted, canonical-path jailing |
| bash | real | `sh -c`, timeout kill (124), workdir gate |
| task | real | foreground/background/task_id, depth guard |
| todowrite | real | process-wide list (session scope deferred) |
| webfetch | real | OkHttp, 200k cap |
| skill | real | SKILL.md discovery |
| question | stub | loop-driven CLI question flow pending |
| websearch | stub | needs provider key |
| apply_patch/lsp/plan_exit | stub | documented reasons in code |
| MCP `server.tool` | real | via McpManager |

Lifecycle (all real tools): repair name -> permission (deny/ask/allow) -> external-dir gate -> arg validation -> execute -> truncate -> context. See PHASE-4-TOOLS.md.
