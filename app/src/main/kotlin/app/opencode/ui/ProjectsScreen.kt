package app.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.opencode.workspace.Project
import app.opencode.workspace.WorkspaceManager
import java.io.File

@Composable
fun ProjectsScreen(onOpen: (Project) -> Unit) {
    val ctx = LocalContext.current
    val ws = remember { WorkspaceManager(File(ctx.filesDir, "projects").apply { mkdirs() }) }
    var names by remember { mutableStateOf(ws.list().map { it.name }) }
    var error by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Text("Projects")
        Row {
            TextField(draft, { draft = it }, Modifier.weight(1f), label = { Text("name") })
            Button(onClick = {
                try {
                    ws.create(draft)
                    names = ws.list().map { it.name }
                    draft = ""
                    error = ""
                } catch (e: Exception) {
                    error = e.message ?: "invalid"
                }
            }) { Text("New") }
        }
        if (error.isNotEmpty()) Text("error: $error")
        LazyColumn {
            items(names) { n ->
                Button(
                    onClick = { ws.open(File(ws.base(), n))?.let(onOpen) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) { Text(n) }
            }
        }
    }
}
