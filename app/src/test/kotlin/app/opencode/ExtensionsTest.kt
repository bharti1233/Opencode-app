package app.opencode

import app.opencode.mcp.McpServer
import app.opencode.mcp.StdioTransport
import app.opencode.mcp.buildRequest
import app.opencode.skill.SkillTool
import app.opencode.skill.discoverSkills
import app.opencode.skill.parseSkill
import app.opencode.tools.WebfetchTool
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExtensionsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun skillFrontmatter() {
        val dir = tmp.newFolder("skills", "review")
        java.io.File(dir, "SKILL.md").writeText("---\nname: review\ndescription: Review code\n---\n\nBody here")
        val found = discoverSkills(listOf(tmp.root))
        assertEquals(listOf("review"), found.map { it.name })
        assertEquals("Review code", found.single().description)
        assertEquals(null, parseSkill(java.io.File(dir, "missing.md")))
    }

    @Test fun skillToolLoads() = runBlocking {
        val dir = tmp.newFolder("s")
        java.io.File(dir, "SKILL.md").writeText("---\nname: x\n---\nContent-X")
        val ok = SkillTool(listOf(tmp.root)).execute(mapOf("name" to "x"))
        assertTrue(ok.output.contains("Content-X"))
        val miss = SkillTool(listOf(tmp.root)).execute(mapOf("name" to "nope"))
        assertTrue((miss.error ?: "").contains("unknown skill"))
    }

    @Test fun jsonRpcFraming() {
        val raw = buildRequest(7, "tools/list", null)
        val o = JSONObject(raw)
        assertEquals("2.0", o.getString("jsonrpc"))
        assertEquals(7, o.getLong("id"))
        assertEquals("tools/list", o.getString("method"))
    }

    @Test fun stdioScriptServer() {
        // Minimal fake MCP server: answers every request with {ok:true},
        // exercising spawn, newline framing, id matching, blank skipping.
        val script = "while IFS= read -r line; do " +
            "id=\$(printf '%s' \"\$line\" | sed -n 's/.*\"id\"[ ]*:[ ]*\\([0-9][0-9]*\\).*/\\1/p'); " +
            "printf '{\"jsonrpc\":\"2.0\",\"id\":%s,\"result\":{\"ok\":true}}\\n' \"\$id\"; " +
            "done"
        val t = StdioTransport(McpServer("fake", command = listOf("sh", "-c", script)))
        try {
            val res = t.request("ping", JSONObject().put("a", 1))
            assertEquals(true, res.optBoolean("ok"))
        } finally {
            t.close()
        }
    }

    class FakeTransport(val handler: (String, JSONObject?) -> JSONObject) : app.opencode.mcp.McpTransport {
        val calls = mutableListOf<String>()
        override fun request(method: String, params: JSONObject?) =
            handler(method, params).also { calls += method }
        override fun close() {}
    }

    @Test fun mcpClientParsing() {
        val transport = FakeTransport { method, _ ->
            when (method) {
                "tools/list" -> JSONObject().put(
                    "tools",
                    org.json.JSONArray().put(
                        JSONObject().put("name", "t").put("description", "d")
                            .put("inputSchema", JSONObject().put("type", "object")),
                    ),
                )
                else -> JSONObject().put(
                    "content",
                    org.json.JSONArray().put(JSONObject().put("type", "text").put("text", "hi"))
                        .put(JSONObject().put("type", "text").put("text", "yo")),
                )
            }
        }
        val client = app.opencode.mcp.McpClient(McpServer("srv"), transport)
        val defs = client.listTools()
        assertEquals("t", defs.single().name)
        assertEquals("d", defs.single().description)
        assertEquals("{\"type\":\"object\"}", defs.single().inputSchemaJson)
        assertEquals("hi\nyo", client.callTool("t", "{}"))
        val mgr = app.opencode.mcp.McpManager().add(client)
        val tools = mgr.tools()
        assertEquals(listOf("srv.t"), tools.keys.toList())
        assertEquals("mcp_srv_t", tools.values.single().permission)
    }

    @Test fun mcpCallError() {
        val transport = FakeTransport { _, _ ->
            JSONObject().put("isError", true).put(
                "content",
                org.json.JSONArray().put(JSONObject().put("type", "text").put("text", "bad")),
            )
        }
        val client = app.opencode.mcp.McpClient(McpServer("srv"), transport)
        try {
            client.callTool("t", "{}")
            org.junit.Assert.fail("expected throw")
        } catch (e: IllegalStateException) {
            assertEquals("bad", e.message)
        }
    }

    @Test fun webfetchLocalhost() = runBlocking {
        val server = okhttp3.mockwebserver.MockWebServer()
        server.enqueue(okhttp3.mockwebserver.MockResponse().setBody("hello-fetch"))
        server.start()
        try {
            val ok = WebfetchTool().execute(mapOf("url" to server.url("/").toString()))
            assertEquals("hello-fetch", ok.output)
            server.shutdown()
            val bad = WebfetchTool().execute(mapOf("url" to server.url("/").toString()))
            assertTrue((bad.error ?: "").isNotEmpty())
        } finally {
            runCatching { server.shutdown() }
        }
    }
}
