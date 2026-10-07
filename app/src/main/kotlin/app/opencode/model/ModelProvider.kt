package app.opencode.model

// Port of provider/provider.ts (catalog-driven selection; streaming + tool-calling in Phase 3).
// Bundled provider ids mirror BUNDLED_PROVIDERS; keys via Android Keystore (Phase 3).

data class ModelRef(val providerId: String, val modelId: String, val variant: String? = null) {
    override fun toString() = if (variant != null) "$providerId/$modelId#$variant" else "$providerId/$modelId"
}

interface ModelProvider {
    val id: String
    suspend fun listModels(): List<String>
}

object ProviderCatalog {
    // ponytail: subset first (anthropic/openai/google/openrouter); full ~20 in Phase 3.
    val bundled: List<String> = listOf(
        "anthropic", "openai", "google", "openrouter", "xai", "mistral",
        "groq", "deepinfra", "cerebras", "togetherai", "azure", "bedrock",
    )
    fun parse(ref: String): ModelRef {
        val provider = ref.substringBefore("/", missingDelimiterValue = "")
        val rest = ref.substringAfter("/", missingDelimiterValue = ref)
        val model = rest.substringBefore("#")
        val variant = rest.substringAfter("#", missingDelimiterValue = "").ifEmpty { null }
        return ModelRef(provider.ifEmpty { "anthropic" }, model, variant)
    }
}
