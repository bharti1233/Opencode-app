package app.opencode

import app.opencode.permissions.Action
import app.opencode.permissions.Rule as PermRule
import app.opencode.terminal.CommandResult
import app.opencode.terminal.ProcTerminalManager
import app.opencode.terminal.ProcessCommandExecutor
import app.opencode.terminal.TerminalSessionImpl
import app.opencode.tools.ToolRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TerminalTest {
    @get:Rule val tmp = TemporaryFolder()
    private val exec = ProcessCommandExecutor()

    @Test fun echoOk() = runBlocking {
        val r = exec.run("echo hello", tmp.root.path, 10_000)
        assertEquals(0, r.exitCode)
        assertTrue(r.stdout.contains("hello"))
    }

    @Test fun nonZeroExit() = runBlocking {
        val r: CommandResult = exec.run("exit 3", tmp.root.path, 10_000)
        assertEquals(3, r.exitCode)
    }

    @Test fun timeoutKills() = runBlocking {
        val r = exec.run("sleep 10", tmp.root.path, 800)
        assertEquals(124, r.exitCode)
        assertTrue(r.stderr.contains("timed out"))
    }

    @Test fun badDir() = runBlocking {
        val r = exec.run("echo hi", tmp.root.path + "/missing", 10_000)
        assertEquals(-1, r.exitCode)
    }

    @Test fun bashToolViaRunner() = runBlocking {
        val tools = ToolRunner.executors(tmp.root)
        val ok = ToolRunner.run("bash", mapOf("command" to "echo via-tool"), emptyList(), tools) { _, _ -> true }
        assertTrue(ok.output.contains("via-tool"))
        val denied = ToolRunner.run(
            "bash", mapOf("command" to "echo no"),
            listOf(PermRule("bash", "echo *", Action.DENY)), tools,
        )
        assertTrue((denied.error ?: "").contains("denied"))
    }

    @Test fun interactiveSession() {
        val mgr = ProcTerminalManager()
        val s = mgr.open(tmp.root.path) as TerminalSessionImpl
        assertEquals(1, mgr.list().size)
        s.sendInput("echo marker-term-xyz")
        val deadline = System.currentTimeMillis() + 5000
        var out = ""
        while (System.currentTimeMillis() < deadline) {
            out = s.output()
            if (out.contains("marker-term-xyz")) break
            Thread.sleep(100)
        }
        assertTrue(out.contains("marker-term-xyz"))
        s.cancel()
        assertEquals(false, s.isAlive())
    }
}
