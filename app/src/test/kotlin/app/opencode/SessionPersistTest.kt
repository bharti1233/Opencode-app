package app.opencode

import app.opencode.session.Message
import app.opencode.session.Part
import app.opencode.session.SessionTables
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPersistTest {
    @Test fun tableRoundtrip() {
        val m = Message(
            "m1", "s1", "assistant",
            listOf(
                Part.Text("hello"),
                Part.ToolCall("c1", "read", "completed", """{"filePath":"a"}"""),
                Part.ToolResult("c1", "bytes", null),
            ),
        )
        val (me, ps) = SessionTables.toRows(m, 3)
        assertEquals(3, me.seq)
        assertEquals(3, ps.size)
        val back = SessionTables.fromRows(me, ps)
        assertEquals(m, back)
    }

    @Test fun toolResultErrorSurvives() {
        val m = Message("m2", "s1", "tool", listOf(Part.ToolResult("c9", "", "boom")))
        val (me, ps) = SessionTables.toRows(m, 0)
        val back = SessionTables.fromRows(me, ps)
        assertEquals("boom", (back.parts.single() as Part.ToolResult).error)
    }
}
