package app.opencode.mcp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.util.concurrent.TimeUnit

// Port of mcp/index.ts transports (stdio + streamable-HTTP fallback shape).
// OAuth/client-registration flows deferred; servers needing auth send headers.

data class McpServer(
    val name: String,
    val command: List<String>? = null,
    val cwd: String? = null,
    val env: Map<String, String> = emptyMap(),
    val url: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 30_000,
)

data class McpToolDef(val name: String, val description: String, val inputSchemaJson: String)

fun buildRequest(id: Long, method: String, params: JSONObject?): String {
    val o = JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method)
    if (params != null) o.put("params", params)
    return o.toString()
}

interface McpTransport : Closeable {
    fun request(method: String, params: JSONObject?): JSONObject
}

class StdioTransport(server: McpServer) : McpTransport {
    private val proc: Process
    private val out: BufferedReader
    private var nextId = 1L

    init {
        require(!server.command.isNullOrEmpty()) { "stdio server needs command" }
        val pb = ProcessBuilder(server.command)
        server.cwd?.let { pb.directory(File(it)) }
        pb.environment().putAll(server.env)
        proc = pb.start()
        out = proc.inputStream.bufferedReader()
        // initialize handshake (result ignored beyond surfacing errors)
        request("initialize", JSONObject().put("protocolVersion", "2024-11-05")
            .put("capabilities", JSONObject()).put("clientInfo", JSONObject().put("name", "opencode-android").put("version", "0.1.0")))
    }

    @Synchronized
    override fun request(method: String, params: JSONObject?): JSONObject {
        val id = nextId++
        proc.outputStream.write((buildRequest(id, method, params) + "\n").toByteArray())
        proc.outputStream.flush()
        val deadline = System.currentTimeMillis() + 30_000
        while (true) {
            if (System.currentTimeMillis() > deadline) throw IllegalStateException("mcp timeout: $method")
            val line = out.readLine() ?: throw IllegalStateException("mcp server closed stdout")
            if (line.isBlank()) continue
            val o = JSONObject(line)
            if (o.optLong("id", -1) != id) continue // notifications/interleaved
            if (o.has("error") && !o.isNull("error")) throw IllegalStateException(o.get("error").toString())
            return o.optJSONObject("result") ?: JSONObject()
        }
    }

    override fun close() {
        try {
            proc.destroyForcibly()
        } catch (_: Exception) {
        }
    }
}

class HttpTransport(
    private val server: McpServer,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build(),
) : McpTransport {
    private var nextId = 1L

    override fun request(method: String, params: JSONObject?): JSONObject {
        val id = nextId++
        val req = Request.Builder()
            .url(server.url!!)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .apply { server.headers.forEach { (k, v) -> header(k, v) } }
            .post(buildRequest(id, method, params).toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { res ->
            val body = (res.body ?: throw IllegalStateException("empty body")).string()
            val data = if (body.contains("text/event-stream") || body.startsWith("event:")) {
                body.lines().filter { it.startsWith("data:") }.joinToString("") { it.removePrefix("data:").trim() }
            } else body
            val o = JSONObject(data)
            if (o.has("error") && !o.isNull("error")) throw IllegalStateException(o.get("error").toString())
            return o.optJSONObject("result") ?: JSONObject()
        }
    }

    override fun close() {}
}

class McpClient(val server: McpServer, private val transport: McpTransport) : Closeable {
    fun listTools(): List<McpToolDef> {
        val result = transport.request("tools/list", null)
        val arr = result.optJSONArray("tools") ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val t = arr.getJSONObject(i)
            McpToolDef(
                t.getString("name"),
                t.optString("description", ""),
                (t.optJSONObject("inputSchema") ?: JSONObject()).toString(),
            )
        }
    }

    fun callTool(name: String, argsJson: String): String {
        val result = transport.request(
            "tools/call",
            JSONObject().put("name", name).put("arguments", JSONObject(argsJson.ifBlank { "{}" })),
        )
        val content = result.optJSONArray("content") ?: JSONArray()
        val text = (0 until content.length()).mapNotNull { i ->
            val b = content.getJSONObject(i)
            if (b.optString("type", "") == "text") b.optString("text", "") else null
        }.joinToString("\n")
        if (result.optBoolean("isError", false)) throw IllegalStateException(text)
        return text
    }

    override fun close() = transport.close()
}
