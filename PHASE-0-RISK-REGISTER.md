# Phase 0 — Risk Register

| # | Risk | Likelihood / Impact | Mitigation |
|---|---|---|---|
| 1 | ripgrep binary missing on Android (`binary.ts` throws) | High / High (glob/grep dead) | System `rg` dep + vendor aarch64 binary + Kotlin fallback; fail-soft with clear error |
| 2 | Process lifecycle (no -pid group kill, `detached`, Doze kills long runs) | High / High | `detached:false` + plain kill; foreground service + WorkManager; timeout defaults; document limits |
| 3 | Bun-only APIs (`bun:sqlite`, `Bun.hash`, wasm import) | High / Med | Node/Room path only; `node:crypto` hash; Node-compatible wasm loader |
| 4 | Provider API drift (models.dev catalog, ~20 providers) | Med / High | Pin catalog snapshot; per-provider tests; subset first (anthropic/openai/google/openrouter), expand later |
| 5 | Tool-call schema mismatch (repair lowercases to `invalid`) | Med / Med | Port zod->JSON schema + `repairToolCall` equivalent; unit tests per tool schema |
| 6 | Permission over/under-blocking (doom-loop guard, external_directory) | Med / High | Port wildcard matcher exactly; instrument asks; UI shows pattern+action |
| 7 | Context overflow on small screens/long sessions | Med / Med | Port overflow/compaction/prune thresholds; Room-backed history; streaming UI |
| 8 | Credential leakage (API keys, GH token) | Low / Critical | Keystore/EncryptedSharedPreferences; never log keys; CI secrets only; audit `grep -r ghp_\|sk-` pre-push |
| 9 | Scope creep (22 phases incl. desktop/electron) | High / Med | OPTIONAL list enforced; ponytail rule: deletion first, smallest diff |
| 10 | CI-only verification (no local builds per user constraint) | Med / Med | Slow iteration; keep workflow minimal + cache; diagnose from logs only, one fix per push |
