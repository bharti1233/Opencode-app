package app.opencode.tools

// Port of tool/truncate.ts + tool_output limits.

object Truncate {
    const val MAX_LINES = 2000
    const val MAX_CHARS = 50_000

    fun apply(text: String): Pair<String, Boolean> {
        var t = text
        var truncated = false
        if (t.count { it == '\n' } + 1 > MAX_LINES) {
            t = t.lines().take(MAX_LINES).joinToString("\n")
            truncated = true
        }
        if (t.length > MAX_CHARS) {
            t = t.take(MAX_CHARS)
            truncated = true
        }
        return t to truncated
    }
}
