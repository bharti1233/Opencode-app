package app.opencode

import app.opencode.model.AnthropicStreamState
import app.opencode.model.ChatMessage
import app.opencode.model.ChatRequest
import app.opencode.model.OpenAIStreamState
import app.opencode.model.ProviderCatalog
import app.opencode.model.Providers
import app.opencode.model.RequestedToolCall
import app.opencode.model.Retry
import app.opencode.model.SecureKeys
import app.opencode.model.StreamEvent
import app.opencode.model.ToolSpec
import app.opencode.model.applyAnthropicEvent
import app.opencode.model.applyChunk
import app.opencode.model.buildAnthropicJson
import app.opencode.model.buildRequestJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ModelTest {
    @Test fun retryCodes() {
        assertEquals(true, Retry.isRetryable(429, null))
        assertEquals(true, Retry.isRetryable(500, null))
        assertEquals(true, Retry.isRetryable(503, null))
        assertEquals(false, Retry.isRetryable(400, null))
        assertEquals(false, Retry.isRetryable(401, null))
        assertEquals(true, Retry.isRetryable(null, "rate limit exceeded"))
        assertEquals(false, Retry.isRetryable(null, "bad request"))
    }

    @Test fun retryDelays() {
        assertEquals(2500L, Retry.delayFor(1, null))
        assertEquals(5000L, Retry.delayFor(2, null))
        assertEquals(5000L, Retry.delayFor(1, 5000L))
        assertEquals(Retry.CAP_MS, Retry.delayFor(20, null))
        assertEquals(Retry.CAP_MS, Retry.delayFor(1, 999_000L))
        assertEquals(Retry.MAX_RETRIES, 5)
    }

    @Test fun overflowDetection() {
        assertEquals(true, Retry.isOverflow("context_length_exceeded"))
        assertEquals(true, Retry.isOverflow("Maximum context length reached"))
        assertEquals(false, Retry.isOverflow("bad request"))
        assertEquals(false, Retry.isOverflow(null))
    }

    @Test fun providerEndpoints() {
        assertEquals("https://api.openai.com/v1", Providers.baseUrl("openai"))
        assertEquals("https://api.anthropic.com/v1", Providers.baseUrl("anthropic"))
        try {
            Providers.baseUrl("nope")
            fail("expected throw")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals("anthropic", ProviderCatalog.parse("anthropic/claude-x").providerId)
        assertEquals("openai/gpt-5", ProviderCatalog.parse("openai/gpt-5").toString())
    }

    @Test fun openAIRequestShape() {
        val req = ChatRequest(
            model = "gpt-5",
            messages = listOf(ChatMessage("user", "hi")),
            tools = listOf(ToolSpec("read", "Read a file")),
            system = "sys",
        )
        val j = buildRequestJson(req)
        assertEquals("gpt-5", j.getString("model"))
        assertEquals(true, j.getBoolean("stream"))
        assertEquals("sys", j.getJSONArray("messages").getJSONObject(0).getString("content"))
        assertEquals("read", j.getJSONArray("tools").getJSONObject(0).getJSONObject("function").getString("name"))
    }

    @Test fun openAIChunkFlow() {
        val s = OpenAIStreamState()
        val e1 = applyChunk(s, """{"choices":[{"delta":{"content":"hel"},"finish_reason":null}]}""")
        assertEquals(listOf(StreamEvent.TextDelta("hel")), e1)
        applyChunk(s, """{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"c1","function":{"name":"read","arguments":"{\"file"}}]}}]}""")
        applyChunk(s, """{"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"Path\"}"}}]}}]}""")
        val done = applyChunk(s, "[DONE]")
        assertEquals(2, done.size)
        val tc = done[0] as StreamEvent.ToolCall
        assertEquals("c1", tc.call.id)
        assertEquals("read", tc.call.name)
        assertEquals("{\"filePath\"}", tc.call.argumentsJson)
        assertTrue(done[1] is StreamEvent.Done)
    }

    @Test fun anthropicEventFlow() {
        val s = AnthropicStreamState()
        applyAnthropicEvent(s, "content_block_start", """{"index":0,"content_block":{"type":"tool_use","id":"t1","name":"edit"}}""")
        val t = applyAnthropicEvent(s, "content_block_delta", """{"index":0,"delta":{"type":"text_delta","text":"ok"}}""")
        assertEquals(listOf(StreamEvent.TextDelta("ok")), t)
        applyAnthropicEvent(s, "content_block_delta", """{"index":0,"delta":{"type":"input_json_delta","partial_json":"{\"a\":1}"}}""")
        val done = applyAnthropicEvent(s, "message_stop", "{}")
        val tc = done[0] as StreamEvent.ToolCall
        assertEquals("t1", tc.call.id)
        assertEquals("{\"a\":1}", tc.call.argumentsJson)
    }

    @Test fun secureKeysRoundtrip() {
        val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        val blob = SecureKeys.encrypt(key, "sk-secret".toByteArray())
        assertEquals("sk-secret", SecureKeys.decrypt(key, blob).decodeToString())
        // wrong key must fail, not return garbage
        val other = keyGen.generateKey()
        try {
            SecureKeys.decrypt(other, blob)
            fail("expected auth failure")
        } catch (_: Exception) {
        }
    }

    @Test fun anthropicToolResultShape() {
        val j = buildAnthropicJson(
            ChatRequest(
                model = "m",
                messages = listOf(
                    ChatMessage("assistant", "", toolCalls = listOf(RequestedToolCall("t1", "read", "{}"))),
                    ChatMessage("tool", "content", toolCallId = "t1"),
                ),
            ),
        )
        val msgs = j.getJSONArray("messages")
        assertEquals("tool_use", msgs.getJSONObject(0).getJSONArray("content").getJSONObject(0).getString("type"))
        assertEquals("tool_result", msgs.getJSONObject(1).getJSONArray("content").getJSONObject(0).getString("type"))
    }
}
