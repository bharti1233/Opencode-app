# Phase 3 — Model/Provider System

Port of `provider/provider.ts` + `session/retry.ts` + `session/llm/*` (streaming/timeouts).

## What landed

- `model/ChatModel.kt` — shared request/event shapes (ToolSpec, ChatMessage w/ tool_calls + tool results, ChatRequest, StreamEvent TextDelta/ToolCall/Done/Error).
- `model/OpenAICompatClient.kt` — chat-completions + SSE via okhttp-sse, 300s read timeout, delta accumulation across chunks, usage capture. Covers openai/openrouter/xai/groq/mistral/deepinfra/togetherai/cerebras + google (compat endpoint).
- `model/AnthropicClient.kt` — Messages API + typed SSE events (content_block_start/delta, message_delta/stop, error), tool_use/input_json accumulation.
- `model/Providers.kt` — id -> default endpoint factory. azure/bedrock/vertex throw explicit scope-out (need platform auth).
- `model/Retry.kt` — max 5, retry-after honored else 2s*2^(n-1) cap 30s, 429/5xx + network-pattern retryable, context-overflow classified non-retryable. Deviation: deterministic +25% ceiling instead of random jitter.
- `model/SecureKeys.kt` — EncryptedSharedPreferences (AES256), never logged.
- 9 unit tests (retry codes/delays/overflow, endpoints, both request shapes, both stream parsers, tool-result mapping).

## Deferred

Model catalog download (models.dev), OAuth flows, Azure/Bedrock/Vertex auth, variants/transforms, `repairToolCall` (lands with tool validation, Phase 4), parallel/sequential multi-call orchestration (Phase 8).
