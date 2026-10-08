package app.opencode.model

// Request/event shapes shared by all protocol clients.

data class ToolSpec(
    val name: String,
    val description: String,
    val inputSchemaJson: String = "{\"type\":\"object\",\"properties\":{}}",
)

data class RequestedToolCall(val id: String, val name: String, val argumentsJson: String)

data class ChatMessage(
    val role: String, // system|user|assistant|tool
    val content: String = "",
    val toolCallId: String? = null,
    val toolCalls: List<RequestedToolCall> = emptyList(),
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolSpec> = emptyList(),
    val system: String? = null,
    val maxTokens: Int = 4096,
    val temperature: Double? = null,
)

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data class ToolCall(val call: RequestedToolCall) : StreamEvent
    data class Done(val stopReason: String, val inputTokens: Int = 0, val outputTokens: Int = 0) : StreamEvent
    data class Error(val message: String, val retryable: Boolean) : StreamEvent
}
