package app.opencode.model

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// OpenAI-compatible chat-completions + SSE. Covers openai/openrouter/xai/groq/
// mistral/deepinfra/togetherai/cerebras and google via its compat endpoint.

class OpenAICompatClient(
    override val providerId: String,
    private val baseUrl: String,
    private val apiKey: () -> String?,
    private val extraHeaders: Map<String, String> = emptyMap(),
    private val http: OkHttpClient = defaultHttp(),
) : ModelClient {

    override fun stream(request: ChatRequest): Flow<StreamEvent> = callbackFlow {
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${apiKey() ?: ""}")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
            .post(buildRequestJson(request).toString().toRequestBody("application/json".toMediaType()))
            .build()
        val state = OpenAIStreamState()
        val listener = object : EventSourceListener() {
            override fun onEvent(source: EventSource, id: String?, type: String?, data: String) {
                for (e in applyChunk(state, data)) trySend(e)
                if (data.trim() == "[DONE]") close()
            }

            override fun onFailure(source: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code
                val msg = t?.message ?: "HTTP $code"
                if (Retry.isOverflow(msg)) trySend(StreamEvent.Error("context overflow", false))
                else trySend(StreamEvent.Error(msg, Retry.isRetryable(code, msg)))
                close()
            }
        }
        val source = EventSources.createFactory(http).newEventSource(req, listener)
        awaitClose { source.cancel() }
    }

    companion object {
        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build()
    }
}

class OpenAIStreamState {
    val toolArgs = mutableMapOf<Int, StringBuilder>()
    val toolId = mutableMapOf<Int, String>()
    val toolName = mutableMapOf<Int, String>()
    var stop = "stop"
    var promptTokens = 0
    var completionTokens = 0
}

fun buildRequestJson(r: ChatRequest): JSONObject {
    val root = JSONObject()
    root.put("model", r.model)
    root.put("stream", true)
    root.put("max_tokens", r.maxTokens)
    r.temperature?.let { root.put("temperature", it) }
    val msgs = JSONArray()
    r.system?.let { msgs.put(JSONObject().put("role", "system").put("content", it)) }
    for (m in r.messages) {
        val o = JSONObject().put("role", m.role)
        when {
            m.toolCalls.isNotEmpty() -> {
                o.put("content", m.content)
                val arr = JSONArray()
                for (tc in m.toolCalls) {
                    arr.put(
                        JSONObject().put("id", tc.id).put("type", "function")
                            .put("function", JSONObject().put("name", tc.name).put("arguments", tc.argumentsJson)),
                    )
                }
                o.put("tool_calls", arr)
            }
            m.toolCallId != null -> {
                o.put("content", m.content)
                o.put("tool_call_id", m.toolCallId)
            }
            else -> o.put("content", m.content)
        }
        msgs.put(o)
    }
    root.put("messages", msgs)
    if (r.tools.isNotEmpty()) {
        val arr = JSONArray()
        for (t in r.tools) {
            arr.put(
                JSONObject().put("type", "function")
                    .put(
                        "function",
                        JSONObject().put("name", t.name).put("description", t.description)
                            .put("parameters", JSONObject(t.inputSchemaJson)),
                    ),
            )
        }
        root.put("tools", arr)
    }
    return root
}

fun applyChunk(s: OpenAIStreamState, data: String): List<StreamEvent> {
    val out = mutableListOf<StreamEvent>()
    val d = data.trim()
    if (d == "[DONE]") {
        for ((i, sb) in s.toolArgs) {
            out += StreamEvent.ToolCall(RequestedToolCall(s.toolId[i] ?: "", s.toolName[i] ?: "", sb.toString()))
        }
        out += StreamEvent.Done(s.stop, s.promptTokens, s.completionTokens)
        return out
    }
    val root = JSONObject(d)
    root.optJSONObject("usage")?.let { u ->
        s.promptTokens = u.optInt("prompt_tokens", s.promptTokens)
        s.completionTokens = u.optInt("completion_tokens", s.completionTokens)
    }
    val choice = root.optJSONArray("choices")?.optJSONObject(0) ?: return out
    if (choice.has("finish_reason") && !choice.isNull("finish_reason")) {
        s.stop = choice.optString("finish_reason", s.stop)
    }
    val delta = choice.optJSONObject("delta") ?: return out
    if (delta.has("content") && !delta.isNull("content")) {
        val text = delta.optString("content", "")
        if (text.isNotEmpty()) out += StreamEvent.TextDelta(text)
    }
    val calls = delta.optJSONArray("tool_calls") ?: return out
    for (i in 0 until calls.length()) {
        val tc = calls.getJSONObject(i)
        val idx = tc.optInt("index", 0)
        if (tc.has("id") && !tc.isNull("id")) s.toolId[idx] = tc.getString("id")
        val f = tc.optJSONObject("function") ?: continue
        if (f.has("name") && !f.isNull("name")) s.toolName[idx] = f.getString("name")
        if (f.has("arguments") && !f.isNull("arguments")) {
            s.toolArgs.getOrPut(idx) { StringBuilder() }.append(f.getString("arguments"))
        }
    }
    return out
}
