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

// Anthropic Messages API + SSE (event: + data: pairs; okhttp-sse splits them).

class AnthropicClient(
    override val providerId: String,
    private val baseUrl: String,
    private val apiKey: () -> String?,
    private val http: OkHttpClient = OpenAICompatClient.defaultHttp(),
) : ModelClient {

    override fun stream(request: ChatRequest): Flow<StreamEvent> = callbackFlow {
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/messages")
            .header("x-api-key", apiKey() ?: "")
            .header("anthropic-version", "2023-06-01")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(buildAnthropicJson(request).toString().toRequestBody("application/json".toMediaType()))
            .build()
        val state = AnthropicStreamState()
        val listener = object : EventSourceListener() {
            override fun onEvent(source: EventSource, id: String?, type: String?, data: String) {
                for (e in applyAnthropicEvent(state, type, data)) trySend(e)
                if (type == "message_stop" || type == "error") close()
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
}

class AnthropicStreamState {
    val json = mutableMapOf<Int, StringBuilder>()
    val id = mutableMapOf<Int, String>()
    val name = mutableMapOf<Int, String>()
    var stop = "end_turn"
}

fun buildAnthropicJson(r: ChatRequest): JSONObject {
    val root = JSONObject()
    root.put("model", r.model)
    root.put("max_tokens", r.maxTokens)
    root.put("stream", true)
    r.system?.let { root.put("system", it) }
    val msgs = JSONArray()
    for (m in r.messages) {
        if (m.role == "system") continue
        when {
            m.toolCalls.isNotEmpty() -> {
                val blocks = JSONArray()
                if (m.content.isNotEmpty()) blocks.put(JSONObject().put("type", "text").put("text", m.content))
                for (tc in m.toolCalls) {
                    blocks.put(
                        JSONObject().put("type", "tool_use").put("id", tc.id).put("name", tc.name)
                            .put("input", JSONObject(tc.argumentsJson.ifEmpty { "{}" })),
                    )
                }
                msgs.put(JSONObject().put("role", "assistant").put("content", blocks))
            }
            m.toolCallId != null -> {
                msgs.put(
                    JSONObject().put("role", "user").put(
                        "content",
                        JSONArray().put(
                            JSONObject().put("type", "tool_result").put("tool_use_id", m.toolCallId)
                                .put("content", m.content),
                        ),
                    ),
                )
            }
            else -> msgs.put(JSONObject().put("role", m.role).put("content", m.content))
        }
    }
    root.put("messages", msgs)
    if (r.tools.isNotEmpty()) {
        val arr = JSONArray()
        for (t in r.tools) {
            arr.put(
                JSONObject().put("name", t.name).put("description", t.description)
                    .put("input_schema", JSONObject(t.inputSchemaJson)),
            )
        }
        root.put("tools", arr)
    }
    return root
}

fun applyAnthropicEvent(s: AnthropicStreamState, type: String?, data: String): List<StreamEvent> {
    val out = mutableListOf<StreamEvent>()
    val d = data.trim()
    if (d.isEmpty()) return out
    when (type) {
        "content_block_start" -> {
            val root = JSONObject(d)
            val idx = root.optInt("index", 0)
            val block = root.optJSONObject("content_block") ?: return out
            if (block.optString("type", "") == "tool_use") {
                s.id[idx] = block.optString("id", "")
                s.name[idx] = block.optString("name", "")
            }
        }
        "content_block_delta" -> {
            val root = JSONObject(d)
            val idx = root.optInt("index", 0)
            val delta = root.optJSONObject("delta") ?: return out
            when (delta.optString("type", "")) {
                "text_delta" -> {
                    val text = delta.optString("text", "")
                    if (text.isNotEmpty()) out += StreamEvent.TextDelta(text)
                }
                "input_json_delta" -> {
                    s.json.getOrPut(idx) { StringBuilder() }.append(delta.optString("partial_json", ""))
                }
            }
        }
        "message_delta" -> {
            JSONObject(d).optJSONObject("delta")?.optString("stop_reason", null)
                ?.takeIf { it.isNotEmpty() }?.let { s.stop = it }
        }
        "message_stop" -> {
            for ((i, sb) in s.json) {
                out += StreamEvent.ToolCall(RequestedToolCall(s.id[i] ?: "", s.name[i] ?: "", sb.toString()))
            }
            out += StreamEvent.Done(s.stop)
        }
        "error" -> {
            var msg = d
            try {
                val err = JSONObject(d).optJSONObject("error")
                if (err != null) {
                    val m: String = err.optString("message")
                    if (m.isNotEmpty()) msg = m
                }
            } catch (_: Exception) {
                msg = d
            }
            out += StreamEvent.Error(msg, Retry.isRetryable(null, msg))
        }
    }
    return out
}
