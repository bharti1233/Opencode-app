# Phase 0 — Dependency Audit

- Runtime: `bun@1.3.14` (root packageManager). Dual sqlite (`core/database/sqlite.{bun,node}.ts`) — use node path on Android/JVM (Room).
- AI: `ai@6.0.168` (streamText), per-provider SDK factories (~20 bundled: anthropic/openai/google/bedrock/azure/vertex/openrouter/xai/mistral/groq/deepinfra/cerebras/cohere/gateway/togetherai/perplexity/vercel/alibaba/gitlab/github-copilot/venice) + dynamic npm import for unknown.
- Native/breaking: `@parcel/watcher`, `@lydell/node-pty` (fix-node-pty postinstall), `tree-sitter-bash/powershell`, `web-tree-sitter`, `@silvia-odwyer/photon-node`, `bonjour-service`, `ws`, `google-auth-library`, `@modelcontextprotocol/sdk@1.29.0`, `esbuild`, `drizzle-orm`, `effect` platform-node + sqlite-bun.
- Trusted deps (root `trustedDependencies`): parcel/watcher, node-pty, tree-sitter, photon, bonjour, esbuild, protobufjs, electron.
- Scripts: opencode `test: bun test --timeout 30000 --only-failures`, `test:httpapi`; root `test` refuses (must run per-package); `lint: oxlint`; `typecheck: turbo typecheck`.
- CI (`.github/workflows/`): `test.yml` (unit linux+windows Node 24 + setup-bun + `bun turbo test` + Linux `check:generated` + `test:httpapi`; e2e Playwright chromium `packages/app test:e2e:local`), `typecheck.yml`, `publish.yml`, `models-snapshot.yml`, `nix-*.yml`, `review/pr-standards/triage/duplicate-issues` agent workflows.
- Android mapping: JVM/AGP replace bun/node-pty/parcel-watcher (ProcessBuilder + Room + WorkManager); ripgrep -> system/vendor binary or pure-Kotlin fallback; tree-sitter wasm -> keep server-side or drop scan (ask broadly); MCP SDK -> `io.modelcontextprotocol:kotlin-sdk` (when stable) else SSE/stdio bridge; AI SDK -> direct provider HTTPS + SSE streaming (OkHttp).
