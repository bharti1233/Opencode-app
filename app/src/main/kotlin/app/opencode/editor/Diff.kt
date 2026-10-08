package app.opencode.editor

// Line diff for edit previews (agent edits confirm on filesystem; this only
// renders old vs new). Simple LCS DP; capped sizes (ponytail: naive O(n*m)
// is fine for edited files, large files fall back to no-preview).

sealed interface DiffRow {
    data class Same(val line: String) : DiffRow
    data class Del(val line: String) : DiffRow
    data class Add(val line: String) : DiffRow
}

object Diff {
    const val MAX_LINES = 2000

    fun diffLines(old: List<String>, new: List<String>): List<DiffRow>? {
        if (old.size > MAX_LINES || new.size > MAX_LINES) return null
        val n = old.size
        val m = new.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (old[i] == new[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val out = mutableListOf<DiffRow>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                old[i] == new[j] -> {
                    out += DiffRow.Same(old[i])
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    out += DiffRow.Del(old[i])
                    i++
                }
                else -> {
                    out += DiffRow.Add(new[j])
                    j++
                }
            }
        }
        while (i < n) out += DiffRow.Del(old[i++])
        while (j < m) out += DiffRow.Add(new[j++])
        return out
    }
}
