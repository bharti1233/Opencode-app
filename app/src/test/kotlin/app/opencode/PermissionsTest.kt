package app.opencode

import app.opencode.permissions.Action
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionSession
import app.opencode.permissions.Rule as PermRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionsTest {
    @Test fun allowRulePassesWithoutDialog() = runBlocking {
        val s = PermissionSession(mutableListOf(PermRule("bash", "*", Action.ALLOW)))
        var asked = false
        val ok = s.ask("bash", "ls") { asked = true; Decision.DENY }
        assertEquals(true, ok)
        assertEquals(false, asked)
    }

    @Test fun denyRuleBlocksWithoutDialog() = runBlocking {
        val s = PermissionSession(mutableListOf(PermRule("bash", "rm *", Action.DENY)))
        var asked = false
        val ok = s.ask("bash", "rm -rf /") { asked = true; Decision.ALLOW_ONCE }
        assertEquals(false, ok)
        assertEquals(false, asked)
    }

    @Test fun askApproveReject() = runBlocking {
        val s = PermissionSession(mutableListOf())
        assertEquals(true, s.ask("edit", "a.txt") { Decision.ALLOW_ONCE })
        assertEquals(false, s.ask("edit", "a.txt") { Decision.DENY })
    }

    @Test fun alwaysPersistsRule() = runBlocking {
        val s = PermissionSession(mutableListOf())
        var dialogs = 0
        assertEquals(true, s.ask("read", "a.txt") { dialogs++; Decision.ALLOW_ALWAYS })
        assertEquals(1, dialogs)
        assertEquals(true, s.ask("read", "a.txt") { dialogs++; Decision.DENY })
        assertEquals(1, dialogs) // second ask auto-allowed, no dialog
        assertEquals(1, s.rules.size)
    }
}
