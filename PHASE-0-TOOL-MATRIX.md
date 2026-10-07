# Phase 0 — Tool Matrix

Builtin registry `packages/opencode/src/tool/registry.ts:209-249`. All tools: `Tool.define/wrap` (zod schema -> JSONSchema), `Schema.decodeUnknownEffect` validation -> `InvalidArgumentsError`, `ctx.ask` permission, truncate via `Truncate.output`, `Effect.orDie`.

| id | file | inputs | permission |
|---|---|---|---|
| invalid (fallback) | invalid.ts:9 | tool, error | none |
| question | question.ts:15 (CLI/app/desktop only) | questions[] | none (own question.ask event) |
| bash/shell | shell.ts:338-339, shell/prompt.ts:17-20 | command, timeout?, workdir?, description? | bash + external_directory (tree-sitter scan + BashArity.prefix) |
| read | read.ts:64 (args :28-36) | filePath, offset?, limit? | read [rel(path)] |
| glob | glob.ts:17 | pattern, path? | glob [pattern] |
| grep | grep.ts:20 | pattern, path?, include? | grep [pattern] |
| edit | edit.ts:58 | filePath, oldString, newString, replaceAll? | edit [rel(path)] |
| write | write.ts:27 | content, filePath | edit [rel(path)] |
| apply_patch (GPT-only, mutually exclusive w/ edit+write :297-300) | apply_patch.ts:22 | patchText | edit |
| task (subagents) | task.ts:24,81 | description, prompt, subagent_type, task_id?, command?, background? | task [subagent_type] |
| webfetch | webfetch.ts:24 | url, format?, timeout? | webfetch [url] |
| websearch (gated :292) | websearch.ts:99 | query, numResults?, livecrawl?, type?, contextMaxCharacters? | websearch [query] |
| todowrite | todo.ts:14 | todos[] | todowrite [*] |
| skill | skill.ts:12 | name | skill [name] |
| execute (code-mode, experimental flag only) | code-mode.ts:12,188 | code | per inner MCP tool |
| lsp (experimental flag only) | lsp.ts:37 | operation, filePath, line, character, query? | lsp [*] |
| plan_exit (plan-mode CLI only) | plan.ts:15 | {} | none (question.ask) |
| MCP dynamic | session/tools.ts:390 | per-server schema | mcp_tool_key [*] |
| MCP resources | session/tools.ts:27-31 | server?, uri | read [mcp:server:*\|uri] |
| custom/plugin | tool/tools/* + plugin.tool (:184-204) | author-defined | author-defined |

Lifecycle: resolve (registry + MCP, agent/session permission merge, deny-hides-tool) -> validate -> ask -> execute (plugin before/after hooks) -> complete/failToolCall -> context.
File tools also `assertExternalDirectoryEffect` on escape. Doom-loop guard: 3 identical calls -> permission.ask(doom_loop) (`processor.ts:358`).
