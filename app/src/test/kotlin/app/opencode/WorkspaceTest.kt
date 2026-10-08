package app.opencode

import app.opencode.workspace.WorkspaceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WorkspaceTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun createList() {
        val ws = WorkspaceManager(File(tmp.root, "projects"))
        assertEquals(0, ws.list().size)
        val p = ws.create("demo")
        assertEquals(false, p.external)
        assertEquals(listOf("demo"), ws.list().map { it.name })
        try {
            ws.create("../escape")
            fail("expected reject")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test fun openInternalVsExternal() {
        val ws = WorkspaceManager(File(tmp.root, "projects").apply { mkdirs() })
        val inside = File(ws.base(), "a").apply { mkdirs() }
        assertEquals(false, ws.open(inside)!!.external)
        assertEquals(true, ws.open(tmp.root)!!.external)
        assertNull(ws.open(File(tmp.root, "missing")))
    }
}
