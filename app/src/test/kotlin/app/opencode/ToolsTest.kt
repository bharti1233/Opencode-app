package app.opencode

import app.opencode.permissions.Action
import app.opencode.permissions.Rule
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

    @Test fun readWriteRoundtrip() = runBlocking {
        val r = ToolRunner.run("write", mapOf("filePath" to "a.txt", "content" to "hello"), emptyList(), exec())
        assertEquals(null, r.error)
        val back = ToolRunner.run("read", mapOf("filePath" to "a.txt"), emptyList(), exec())
        assertEquals("hello", back.output)
    }

    @Test fun readDirLists() = runBlocking {
        ToolRunner.run("write", mapOf("filePath" to "sub/b.txt", "content" to "x"), emptyList(), exec())
        val out = ToolRunner.run("read", mapOf("filePath" to "sub"), emptyList(), exec())
        assertEquals("b.txt", out.output)
    }

    @Test fun editReplace() = runBlocking {
        ToolRunner.run("write", mapOf("filePath" to "c.txt", "content" to "foo bar"), emptyList(), exec())
        val e = ToolRunner.run(
            "edit",
            mapOf("filePath" to "c.txt", "oldString" to "bar", "newString" to "baz"),
            emptyList(), exec(),
        )
        assertEquals(null, e.error)
        assertEquals("foo baz", ToolRunner.run("read", mapOf("filePath" to "c.txt"), emptyList(), exec()).output)
        val miss = ToolRunner.run(
            "edit", mapOf("filePath" to "c.txt", "oldString" to "zzz", "newString" to "q"),
            emptyList(), exec(),
        )
        assertTrue((miss.error ?: "").contains("not found"))
    }

    @Test fun globGrep() = runBlocking {
        ToolRunner.run("write", mapOf("filePath" to "src/Main.kt", "content" to "fun main() {}"), emptyList(), exec())
        val g = ToolRunner.run("glob", mapOf("pattern" to "*.kt"), emptyList(), exec())
        assertTrue(g.output.contains("Main.kt"))
        val s = ToolRunner.run("grep", mapOf("pattern" to "fun main"), emptyList(), exec())
        assertTrue(s.output.contains("Main.kt:1:"))
    }

    @Test fun denyBlocks() = runBlocking {
        val rules = listOf(Rule("edit", "*", Action.DENY))
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
        val r = ToolRunner.run("READ", mapOf("filePath" to "q.txt"), emptyList(), exec())
        assertTrue((r.error ?: "").contains("no such file")) // repaired to read, then executed
        val bad = ToolRunner.run("frobnicate", emptyMap(), emptyList(), exec())
        assertTrue((bad.error ?: "").contains("unknown tool"))
    }

    @Test fun externalNeedsPermission() = runBlocking {
        val outside = "../outside-phase4.txt"
        val denied = ToolRunner.run("write", mapOf("filePath" to outside, "content" to "x"), emptyList(), exec())
        assertTrue((denied.error ?: "").contains("external directory"))
    }

    @Test fun truncateCaps() {
        val (text, trunc) = Truncate.apply(List(3000) { "line" }.joinToString("\n"))
        assertEquals(true, trunc)
        assertEquals(2000, text.lines().size)
        assertEquals(false, Truncate.apply("short").second)
    }
}
