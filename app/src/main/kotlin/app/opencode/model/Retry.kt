package app.opencode.model

// Port of packages/opencode/src/session/retry.ts.
// max 5 retries; retry-after honored else 2s*2^(n-1) capped at 30s.
// Deviation: deterministic +25% ceiling instead of random jitter (testable).

object Retry {
    const val MAX_RETRIES = 5
    const val BASE_DELAY_MS = 2000L
    const val CAP_MS = 30_000L

    fun isRetryable(httpCode: Int?, message: String?): Boolean {
        if (httpCode != null) {
            if (httpCode == 429) return true
            if (httpCode in 500..599) return true
            return false
        }
        val m = (message ?: "").lowercase()
        return listOf("rate limit", "rate_limit", "overloaded", "timeout", "temporarily", "econnreset", "socket", "try again")
            .any { it in m }
    }

    fun isOverflow(message: String?): Boolean {
        val m = (message ?: "").lowercase()
        return "context_length_exceeded" in m || "maximum context" in m ||
            ("context" in m && ("too long" in m || "too large" in m))
    }

    fun delayFor(attempt: Int, retryAfterMs: Long?): Long {
        if (retryAfterMs != null) return retryAfterMs.coerceAtMost(CAP_MS)
        val exp = BASE_DELAY_MS * (1L shl (attempt - 1).coerceAtLeast(0))
        return (exp + exp / 4).coerceAtMost(CAP_MS)
    }
}
