package app.opencode

import app.opencode.agent.AgentEvent
import app.opencode.agent.AgentLoop
import app.opencode.model.ChatRequest
import app.opencode.model.ModelClient
import app.opencode.model.RequestedToolCall
import app.opencode.model.StreamEvent
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionSession
import app.opencode.permissions.Rule as PermRule
import app.opencode.permissions.Action
import app.opencode.session.InMemorySessionStore
import app.opencode.tools.ToolRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FakeClient(val handler: (callIndex: Int, req: ChatRequest) -> List<StreamEvent>) : ModelClient {
    override val providerId = "fake"
    var calls = 0
    val lastRequests = mutableListOf<ChatRequest>()
    override fun stream(request: ChatRequest): Flow<StreamEvent> = flow {
        calls++
        lastRequests += request
        handler(calls, request).forEach { emit(it) }
    }
}

class AgentLoopTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun loop(client: ModelClient, onPerm: suspend (app.opencode.permissions.PermissionRequest) -> Decision = { Decision.ALLOW_ONCE }) =
        AgentLoop(
            client = client,
            model = "fake/m",
            tools = ToolRunner.executors(tmp.root),
            permissions = PermissionSession(mutableListOf()),
            store = InMemorySessionStore(),
            onPermission = onPerm,
        )

    @Test fun textOnlyCompletes() = runBlocking {
        val events = loop(FakeClient { _, _ ->
            listOf(StreamEvent.TextDelta("hi"), StreamEvent.Done("stop"))
        }).run("s1", "hello", "sys").toList()
        assertEquals(listOf(AgentEvent.Text("hi"), AgentEvent.Completed("hi")), events)
    }

    @Test fun toolRoundtripFeedsResultBack() = runBlocking {
        java.io.File(tmp.root, "a.txt").writeText("file-bytes")
        var n = 0
        val client = FakeClient { _, _ ->
            n++
            if (n == 1) {
                listOf(StreamEvent.ToolCall(RequestedToolCall("c1", "read", """{"filePath":"a.txt"}""")))
            } else {
                listOf(StreamEvent.TextDelta("got it"), StreamEvent.Done("stop"))
            }
        }
        val l = loop(client)
        val events = l.run("s1", "read it", "sys").toList()
        assertTrue(events.any { it is AgentEvent.ToolStarted && it.name == "read" })
        val fin = events.filterIsInstance<AgentEvent.ToolFinished>().single()
        assertEquals("file-bytes", fin.output)
        assertEquals(AgentEvent.Completed("got it"), events.last())
        // second model call saw the tool result in context
        val secondTurn = client.lastRequests[1].messages
        assertTrue(secondTurn.any { it.role == "tool" && it.content == "file-bytes" })
    }

    @Test fun doomGuardBlocksIdenticalRepeats() = runBlocking {
        val client = FakeClient { _, _ ->
            listOf(StreamEvent.ToolCall(RequestedToolCall("c", "bash", """{"command":"echo x"}""")))
        }
        val events = loop(client, onPerm = { Decision.DENY }).run("s1", "go", "sys").toList()
        assertEquals(3, client.calls) // third identical call triggers doom ask -> denied
        assertTrue(events.last() is AgentEvent.Failed)
    }

    @Test fun retryableErrorRetries() = runBlocking {
        var n = 0
        val events = loop(FakeClient { _, _ ->
            n++
            if (n == 1) listOf(StreamEvent.Error("overloaded", true))
            else listOf(StreamEvent.Done("stop"))
        }).run("s1", "hi", "sys").toList()
        assertEquals(AgentEvent.Completed(""), events.last())
    }

    @Test fun denyHidesToolsFromModel() = runBlocking {
        val req = mutableListOf<ChatRequest>()
        val client = FakeClient { _, r -> req += r; listOf(StreamEvent.Done("stop")) }
        val l = AgentLoop(
            client, "fake/m", ToolRunner.executors(tmp.root),
            PermissionSession(mutableListOf(PermRule("edit", "*", Action.DENY))),
            InMemorySessionStore(), { Decision.DENY },
        )
        l.run("s1", "hi", "sys").toList()
        val names = req.single().tools.map { it.name }
        assertEquals(false, names.contains("write"))
        assertEquals(true, names.contains("read"))
    }

    @Test fun parseArgsNumbers() {
        val m = AgentLoop.parseArgs("""{"limit":5,"ok":true,"s":"x"}""")
        assertEquals("5", m["limit"])
        assertEquals("true", m["ok"])
        assertEquals("x", m["s"])
        assertEquals(0, AgentLoop.parseArgs("").size)
    }
}
