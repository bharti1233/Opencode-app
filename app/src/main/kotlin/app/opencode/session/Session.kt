package app.opencode.session

// Port of session row + Message/Part shapes (session/session.ts, schema v1/session.ts).
// Room entities mirror these in Phase 9; in-memory store unblocks the loop now.

data class Session(
    val id: String,
    val projectId: String,
    val directory: String,
    var title: String = "",
    var agent: String = "build",
    var model: String = "",
)

sealed interface Part {
    data class Text(val text: String) : Part
    data class Reasoning(val text: String) : Part
    data class ToolCall(val callId: String, val tool: String, val state: String, val args: String = "") : Part
    data class ToolResult(val callId: String, val output: String, val error: String? = null) : Part
}

data class Message(val id: String, val sessionId: String, val role: String, val parts: List<Part> = emptyList())

interface SessionStore {
    fun create(directory: String): Session
    fun get(id: String): Session?
    fun messages(sessionId: String): List<Message>
    fun append(message: Message)
}

class InMemorySessionStore : SessionStore {
    private val sessions = mutableMapOf<String, Session>()
    private val msgs = mutableMapOf<String, MutableList<Message>>()
    private var n = 0
    override fun create(directory: String): Session {
        val s = Session("ses_${++n}", "prj_1", directory)
        sessions[s.id] = s
        msgs[s.id] = mutableListOf()
        return s
    }
    override fun get(id: String) = sessions[id]
    override fun messages(sessionId: String): List<Message> = msgs[sessionId].orEmpty()
    override fun append(message: Message) { msgs.getOrPut(message.sessionId) { mutableListOf() }.add(message) }
}
