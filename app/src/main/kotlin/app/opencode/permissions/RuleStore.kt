package app.opencode.permissions

import android.content.Context

// Persists user `always` choices (plain prefs — rules are not secrets).

class RuleStore(context: Context) {
    private val prefs = context.getSharedPreferences("opencode_rules", Context.MODE_PRIVATE)

    fun load(): List<Rule> = prefs.getStringSet("rules", emptySet()).orEmpty().mapNotNull { line ->
        val parts = line.split("|", limit = 3)
        if (parts.size != 3) null
        else try {
            Rule(parts[0], parts[1], Action.valueOf(parts[2]))
        } catch (_: Exception) {
            null
        }
    }

    fun save(rules: List<Rule>) {
        prefs.edit()
            .putStringSet("rules", rules.map { "${it.permission}|${it.pattern}|${it.action}" }.toSet())
            .apply()
    }

    fun clear() {
        prefs.edit().remove("rules").apply()
    }
}
