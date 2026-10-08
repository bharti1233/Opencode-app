package app.opencode

import app.opencode.agent.Builtins
import app.opencode.agent.deriveChildRules
import app.opencode.agent.resolveAgent
import app.opencode.model.RequestedToolCall
import app.opencode.model.StreamEvent
import app.opencode.permissions.Action
import app.opencode.permissions.Decision
import app.opencode.permissions.Rule as PermRule
import app.opencode.session.InMemorySessionStore
import app.opencode.tools.TaskTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SubagentsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun resolveAndDerive() {
        assertEquals(Builtins.explore, resolveAgent("explore"))
        assertEquals(null, resolveAgent("nope"))
        val parent = listOf(
            PermRule("bash", "*", Action.DENY),
            PermRule("read", "*", Action.ALLOW),
        )
        val child = deriveChildRules(parent, Builtins.explore)
        assertTrue(child.any { it.permission == "bash" && it.action == Action.DENY }) // inherited
        assertTrue(child.none { it.permission == "read" }) // allows not inherited
        assertTrue(child.any { it.permission == "todowrite" && it.action == Action.DENY })
        assertTrue(child.any { it.permission == "task" && it.action == Action.DENY })
    }

    private fun CoroutineScope.taskTool(
        client: FakeClient,
        store: InMemorySessionStore = InMemorySessionStore(),
    ) = TaskTool(
        defs = Builtins.all.associateBy { it.name },
        client = client, model = "fake/m", root = tmp.root,
        store = store, scope = this,
        onPermission = { Decision.ALLOW_ONCE },
    )

    @Test fun exploreReadsFile() = runBlocking {
        java.io.File(tmp.root, "notes.txt").writeText("child-content")
        var n = 0
        val client = FakeClient { _, req ->
            n++
            if (n == 1) {
                listOf(StreamEvent.ToolCall(RequestedToolCall("c1", "read", """{"filePath":"notes.txt"}""")))
            } else {
                val saw = req.messages.any { it.role == "tool" && it.content == "child-content" }
                listOf(StreamEvent.TextDelta(if (saw) "saw-it" else "blind"), StreamEvent.Done("stop"))
            }
        }
        val out = taskTool(client).execute(
            mapOf("description" to "d", "prompt" to "read notes", "subagent_type" to "explore"),
        )
        assertEquals("saw-it", out.output)
    }

    @Test fun unknownAgentErrors() = runBlocking {
        val out = taskTool(FakeClient { _, _ -> listOf(StreamEvent.Done("stop")) })
            .execute(mapOf("description" to "d", "prompt" to "p", "subagent_type" to "nope"))
        assertTrue((out.error ?: "").contains("unknown agent"))
    }

    @Test fun backgroundThenResume() = runBlocking {
        val client = FakeClient { _, _ -> listOf(StreamEvent.TextDelta("bg-done"), StreamEvent.Done("stop")) }
        val tool = taskTool(client)
        val started = tool.execute(
            mapOf("description" to "d", "prompt" to "p", "subagent_type" to "general", "background" to "true"),
        )
        assertTrue(started.output.startsWith("started task_"))
        val id = started.output.removePrefix("started ")
        val deadline = System.currentTimeMillis() + 5000
        var resumed = tool.execute(
            mapOf("description" to "d", "prompt" to "p", "subagent_type" to "general", "task_id" to id),
        )
        while (resumed.output.startsWith("still running") && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(100)
            resumed = tool.execute(
                mapOf("description" to "d", "prompt" to "p", "subagent_type" to "general", "task_id" to id),
            )
        }
        assertEquals("bg-done", resumed.output)
    }

    @Test fun depthGuardStripsTask() = runBlocking {
        val tool = taskTool(FakeClient { _, _ -> listOf(StreamEvent.Done("stop")) })
        // depth=1 tool spawns children at depth 0, which cannot nest further
        assertEquals(false, tool.childTools(Builtins.general).containsKey("task"))
        // explore allow-list never contained task anyway
        assertEquals(false, tool.childTools(Builtins.explore).containsKey("task"))
        assertTrue(tool.childTools(Builtins.explore).containsKey("read"))
    }
}
