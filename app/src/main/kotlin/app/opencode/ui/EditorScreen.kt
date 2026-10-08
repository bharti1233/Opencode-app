package app.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.opencode.editor.Diff
import app.opencode.editor.DiffRow
import app.opencode.workspace.Project
import java.io.File

// User-driven viewer/editor. User edits need no permission gate (the user is
// the actor); agent edits go through EditTool + PermissionSession and only
// count once the filesystem confirms.

@Composable
fun EditorScreen(project: Project) {
    var files by remember { mutableStateOf(listFiles(project.dir)) }
    var path by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }

    fun open(p: String) {
        path = p
        editing = false
        saved = File(project.dir, p).readText()
    }

    Column(Modifier.padding(16.dp)) {
        Text("Editor — ${project.name}")
        TextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("filter files") })
        LazyColumn(Modifier.weight(1f)) {
            val shown = files.filter { query.isEmpty() || it.contains(query, ignoreCase = true) }.take(200)
            itemsIndexed(shown) { _, f ->
                Button(onClick = { open(f) }, modifier = Modifier.fillMaxWidth()) { Text(f) }
            }
        }
        val current = path
        if (current != null) {
            Text("file: $current")
            if (!editing) {
                val lines = saved.lines()
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(lines) { i, l -> Text("${i + 1}: $l") }
                }
                Row {
                    Button(onClick = {
                        files = listFiles(project.dir)
                    }) { Text("Refresh") }
                    Button(onClick = {
                        draft = saved
                        editing = true
                    }) { Text("Edit") }
                }
            } else {
                TextField(draft, { draft = it }, Modifier.fillMaxWidth().weight(1f), maxLines = 500)
                val preview = Diff.diffLines(saved.lines(), draft.lines())
                if (preview != null) {
                    val adds = preview.count { it is DiffRow.Add }
                    val dels = preview.count { it is DiffRow.Del }
                    Text("preview: +$adds -$dels", color = if (adds + dels > 0) Color.Red else Color.Gray)
                }
                Row {
                    Button(onClick = { editing = false }) { Text("Reject") }
                    Button(onClick = {
                        File(project.dir, current).writeText(draft)
                        saved = draft
                        editing = false
                    }) { Text("Accept") }
                }
            }
        }
    }
}

private fun listFiles(root: File): List<String> =
    root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).path }.sorted().take(2000).toList()
