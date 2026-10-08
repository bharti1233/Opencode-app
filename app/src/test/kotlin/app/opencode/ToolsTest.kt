package app.opencode

import app.opencode.permissions.Action
import app.opencode.permissions.Rule as PermRule
import app.opencode.tools.ToolRunner
import app.opencode.tools.Truncate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ToolsTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun exec() = ToolRunner.executors(tmp.root)

    // Default verdict is ASK; approve it everywhere except the deny/ask tests.
    private suspend fun runApproved(
        name: String,
        args: Map<String, String>,
        rules: List<PermRule> = emptyList(),
    ) = ToolRunner.run(name, args, rules, exec()) { _, _ -> true }

    @Test fun readWriteRoundtrip() = runBlocking {
        val r = runApproved("write", mapOf("filePath" to "a.txt", "content" to "hello"))
        assertEquals(null, r.error)
        val back = runApproved("read", mapOf("filePath" to "a.txt"))
        assertEquals("hello", back.output)
    }

    @Test fun readDirLists() = runBlocking {
        runApproved("write", mapOf("filePath" to "sub/b.txt", "content" to "x"))
        val out = runApproved("read", mapOf("filePath" to "sub"))
        assertEquals("b.txt", out.output)
    }

    @Test fun editReplace() = runBlocking {
        runApproved("write", mapOf("filePath" to "c.txt", "content" to "foo bar"))
        val e = runApproved(
            "edit",
            mapOf("filePath" to "c.txt", "oldString" to "bar", "newString" to "baz"),
        )
        assertEquals(null, e.error)
        assertEquals("foo baz", runApproved("read", mapOf("filePath" to "c.txt")).output)
        val miss = runApproved(
            "edit", mapOf("filePath" to "c.txt", "oldString" to "zzz", "newString" to "q"),
        )
        assertTrue((miss.error ?: "").contains("not found"))
    }

    @Test fun globGrep() = runBlocking {
        runApproved("write", mapOf("filePath" to "src/Main.kt", "content" to "fun main() {}"))
        val g = runApproved("glob", mapOf("pattern" to "*.kt"))
        assertTrue(g.output.contains("Main.kt"))
        val s = runApproved("grep", mapOf("pattern" to "fun main"))
        assertTrue(s.output.contains("Main.kt:1:"))
    }

    @Test fun denyBlocks() = runBlocking {
        val rules = listOf(PermRule("edit", "*", Action.DENY))
        val r = ToolRunner.run("write", mapOf("filePath" to "x.txt", "content" to "y"), rules, exec())
        assertTrue((r.error ?: "").contains("denied"))
        assertEquals(false, java.io.File(tmp.root, "x.txt").exists())
    }

    @Test fun askCallback() = runBlocking {
        val ok = ToolRunner.run(
            "read", mapOf("filePath" to "nope.txt"), emptyList(), exec(),
            onAsk = { _, _ -> true },
        )
        assertTrue((ok.error ?: "").contains("no such file")) // approved, then executed
        val no = ToolRunner.run("read", mapOf("filePath" to "nope.txt"), emptyList(), exec())
        assertTrue((no.error ?: "").contains("rejected"))
    }

    @Test fun repairAndInvalid() = runBlocking {
        val r = runApproved("READ", mapOf("filePath" to "q.txt"))
        assertTrue((r.error ?: "").contains("no such file")) // repaired to read, then executed
        val bad = runApproved("frobnicate", emptyMap())
        assertTrue((bad.error ?: "").contains("unknown tool"))
    }

    @Test fun externalNeedsPermission() = runBlocking {
        val denied = runApproved("write", mapOf("filePath" to "../outside-phase4.txt", "content" to "x"))
        assertTrue((denied.error ?: "").contains("external directory"))
    }

    @Test fun truncateCaps() {
        val (text, trunc) = Truncate.apply(List(3000) { "line" }.joinToString("\n"))
        assertEquals(true, trunc)
        assertEquals(2000, text.lines().size)
        assertEquals(false, Truncate.apply("short").second)
    }
}
