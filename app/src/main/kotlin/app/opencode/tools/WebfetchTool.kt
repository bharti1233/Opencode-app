package app.opencode.tools

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Port of tool/webfetch.ts: URL -> text. HTML is returned raw for now
// (readability extraction deferred); failures are errors, never silent.

class WebfetchTool(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build(),
) : AgentTool {
    override val name = "webfetch"
    override val description = "Fetch a URL as text"
    override val inputSchemaJson =
        """{"type":"object","properties":{"url":{"type":"string"},"timeout":{"type":"number"}},"required":["url"]}"""
    override val permission = "webfetch"
    override val requiredArgs = listOf("url")
    override fun target(args: Map<String, String>) = args["url"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val url = args.getValue("url")
        return try {
            val req = Request.Builder().url(url).header("User-Agent", "opencode-android/0.1").get().build()
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return ToolResult("", error = "HTTP ${res.code}")
                val body = res.body.string()
                if (body.length > 200_000) ToolResult(body.take(200_000), truncated = true)
                else ToolResult(body)
            }
        } catch (e: Exception) {
            ToolResult("", error = e.message ?: "fetch failed")
        }
    }
}
