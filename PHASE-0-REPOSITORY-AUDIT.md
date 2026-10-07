# Phase 0 — Repository Audit (opencode-1.18.35)

Source: `opencode-1.18.35.zip` (75M, 7403 entries), extracted to `/tmp/opencode/opencode-1.18.35` for audit.
Root: bun monorepo (`packageManager: bun@1.3.14`), `type: module`, workspaces `packages/*`.

## Packages (top level)

`app ava?/cli client codemode console containers core desktop docs effect-drizzle-sqlite effect-sqlite-node enterprise function http-recorder httpapi-codegen identity llm opencode plugin protocol schema script sdk sdk-next server session-ui slack stats storybook tui ui web`

## Core agent package: `packages/opencode/src`

`account acp agent auth background bus cli command config control-plane effect env event-manifest.ts event-v2-bridge.ts format git id ide image index.ts installation lsp markdown.d.ts mcp node.ts patch permission plugin project provider question server session share skill snapshot storage sync tool util worktree`

Key files verified locally:
- `packages/opencode/src/agent/agent.ts` (built-in agents), `agent/prompt/*.txt`, `agent/generate.txt`
- `packages/opencode/src/session/prompt.ts` (loop ~1081-1343), `processor.ts` (stream ~641-693), `session.ts` (CRUD), `tools.ts` (resolve ~41-134), `system.ts`, `instruction.ts`, `compaction.ts`, `retry.ts`
- `packages/opencode/src/session/prompt/*.txt` (18 variants: default/anthropic/gpt/gpt-astra/codex/gemini/kimi/meta/trinity/beast/copilot-gpt-5/plan/plan-reminder-anthropic/plan-mode/build-switch)
- `packages/opencode/src/tool/` (24 files: registry.ts tool.ts truncate.ts + read/write/edit/glob/grep/shell/apply_patch/task/todo/webfetch/websearch/skill/question/plan/lsp/code-mode/mcp-websearch/invalid/json-schema/schema/external-directory)
- `packages/opencode/src/provider/provider.ts` (~2000+ lines, catalog-driven), `provider/auth.ts`, `provider/error.ts`
- `packages/opencode/src/permission/{index,arity,evaluate}.ts`, `agent/subagent-permissions.ts`
- `packages/opencode/src/mcp/index.ts`, `skill/{index,discovery}.ts`, `git/index.ts`, `project/{vcs,project}.ts`, `storage/`, `bus/global.ts`, `server/`
- `packages/core/src/v1/config/config.ts`, `core/src/util/wildcard.ts`, `core/fs-util.ts`, `core/shell.ts`, `core/ripgrep/*`, `core/filesystem/watcher.ts`

## Runtime/test/CI

- Runtime: `bun@1.3.14`; native deps: `@parcel/watcher @lydell/node-pty tree-sitter(-bash,-powershell) web-tree-sitter @silvia-odwyer/photon-node bonjour-service ws google-auth-library` (`packages/opencode/package.json:54-153`).
- Tests: `packages/opencode`: `bun test --timeout 30000 --only-failures`; `test:httpapi`. Workflows: `.github/workflows/test.yml` (unit linux+windows + e2e Playwright), `typecheck.yml`.
- Packaging: `install` script targets linux-x64/arm64, darwin-x64/arm64, windows-x64 only — no android target. `nix/opencode.nix` wraps `bun+nodejs` + ripgrep.

## Entry points

`packages/opencode/src/index.ts`, `packages/cli/bin`, `packages/tui` (opentui/solid), `packages/desktop` (electron), `packages/server` (HTTP SSE on 4096), `packages/sdk/js`.
