package app.opencode.model

// Port of provider/provider.ts selection: id -> protocol + default endpoint.
// azure/bedrock/vertex need platform-specific auth (AWS SigV4 / GCP / AAD) —
// explicit error, documented in PHASE-0-ANDROID-COMPATIBILITY.md; add in later phase.

object Providers {
    fun baseUrl(providerId: String): String = when (providerId) {
        "openai" -> "https://api.openai.com/v1"
        "openrouter" -> "https://openrouter.ai/api/v1"
        "xai" -> "https://api.x.ai/v1"
        "groq" -> "https://api.groq.com/openai/v1"
        "mistral" -> "https://api.mistral.ai/v1"
        "deepinfra" -> "https://api.deepinfra.com/v1/openai"
        "togetherai" -> "https://api.together.xyz/v1"
        "cerebras" -> "https://api.cerebras.ai/v1"
        "google" -> "https://generativelanguage.googleapis.com/v1beta/openai"
        "anthropic" -> "https://api.anthropic.com/v1"
        "azure", "bedrock", "vertex" ->
            throw IllegalArgumentException("$providerId needs platform auth (Phase 3 scope-out)")
        else -> throw IllegalArgumentException("unknown provider: $providerId")
    }

    fun create(
        providerId: String,
        apiKey: () -> String?,
        extraHeaders: Map<String, String> = emptyMap(),
    ): ModelClient {
        val base = baseUrl(providerId)
        return if (providerId == "anthropic") AnthropicClient(providerId, base, apiKey)
        else OpenAICompatClient(providerId, base, apiKey, extraHeaders)
    }
}
