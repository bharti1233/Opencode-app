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
import java.net.InetSocketAddress

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

    @Test fun stdioEchoRoundtrip() {
        // `cat` echoes our JSON-RPC line back: exercises real process
        // spawn, newline framing, and id matching (not the MCP protocol).
        val t = StdioTransport(McpServer("echo", command = listOf("cat")))
        try {
            val res = t.request("ping", JSONObject().put("a", 1))
            assertEquals(1, res.optJSONObject("params")?.optInt("a"))
        } finally {
            t.close()
        }
    }

    @Test fun webfetchLocalhost() = runBlocking {
        val server = com.sun.net.httpserver.HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            val body = "hello-fetch".toByteArray()
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val port = server.address.port
            val ok = WebfetchTool().execute(mapOf("url" to "http://127.0.0.1:$port/"))
            assertEquals("hello-fetch", ok.output)
            val bad = WebfetchTool().execute(mapOf("url" to "http://127.0.0.1:1/"))
            assertTrue((bad.error ?: "").isNotEmpty())
        } finally {
            server.stop(0)
        }
    }
}
