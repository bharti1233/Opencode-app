package app.opencode

import app.opencode.permissions.Action
import app.opencode.permissions.PermissionEngine
import app.opencode.permissions.Rule
import app.opencode.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreTest {
    @Test fun wildcardStar() {
        val rules = listOf(Rule("read", "*", Action.ALLOW))
        assertEquals(
            app.opencode.permissions.Verdict.ALLOW,
            PermissionEngine.resolve(rules, "read", "src/Main.kt"),
        )
    }

    @Test fun lastMatchWins() {
        val rules = listOf(
            Rule("bash", "*", Action.ALLOW),
            Rule("bash", "rm *", Action.DENY),
        )
        assertEquals(
            app.opencode.permissions.Verdict.DENY,
            PermissionEngine.resolve(rules, "bash", "rm -rf /"),
        )
    }

    @Test fun defaultAsk() {
        assertEquals(
            app.opencode.permissions.Verdict.ASK,
            PermissionEngine.resolve(emptyList(), "edit", "a.txt"),
        )
    }

    @Test fun coreToolsRegistered() {
        val names = ToolRegistry.names().toSet()
        for (t in listOf("read", "write", "edit", "glob", "grep", "bash", "task", "todowrite", "webfetch", "skill")) {
            assertEquals(true, names.contains(t))
        }
    }

    @Test fun denyHidesTool() {
        val visible = ToolRegistry.visibleTools(setOf("bash")).map { it.name }
        assertEquals(false, visible.contains("bash"))
    }
}
