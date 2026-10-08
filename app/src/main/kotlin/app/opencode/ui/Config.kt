package app.opencode.ui

import android.content.Context

// Provider/model selection (Phase 3 config surface). Keys stay in Keystore.

data class AppConfig(val provider: String, val model: String) {
    companion object {
        val DEFAULT = AppConfig("anthropic", "claude-sonnet-4-20250514")
    }
}

private const val FILE = "opencode_cfg"

fun loadConfig(ctx: Context): AppConfig {
    val p = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    return AppConfig(
        p.getString("provider", AppConfig.DEFAULT.provider) ?: AppConfig.DEFAULT.provider,
        p.getString("model", AppConfig.DEFAULT.model) ?: AppConfig.DEFAULT.model,
    )
}

fun saveConfig(ctx: Context, cfg: AppConfig) {
    ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        .putString("provider", cfg.provider)
        .putString("model", cfg.model)
        .apply()
}
