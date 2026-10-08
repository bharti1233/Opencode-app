package app.opencode

import app.opencode.editor.Diff
import app.opencode.editor.DiffRow
import org.junit.Assert.assertEquals
import org.junit.Test

class DiffTest {
    @Test fun identical() {
        val rows = Diff.diffLines(listOf("a", "b"), listOf("a", "b"))!!
        assertEquals(listOf(DiffRow.Same("a"), DiffRow.Same("b")), rows)
    }

    @Test fun addDel() {
        val rows = Diff.diffLines(listOf("a", "b", "c"), listOf("a", "x", "c", "d"))!!
        assertEquals(
            listOf(DiffRow.Same("a"), DiffRow.Del("b"), DiffRow.Add("x"), DiffRow.Same("c"), DiffRow.Add("d")),
            rows,
        )
    }

    @Test fun emptyOld() {
        assertEquals(listOf(DiffRow.Add("n")), Diff.diffLines(emptyList(), listOf("n")))
    }

    @Test fun tooLarge() {
        assertEquals(null, Diff.diffLines(List(2001) { "a" }, listOf("b")))
    }
}
