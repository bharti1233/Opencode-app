package app.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.opencode.terminal.ProcTerminalManager
import app.opencode.terminal.TerminalSessionImpl
import app.opencode.workspace.Project
import kotlinx.coroutines.delay

@Composable
fun TerminalScreen(project: Project) {
    val mgr = remember { ProcTerminalManager() }
    val sess = remember(project.dir.path) { mgr.open(project.dir.path) as TerminalSessionImpl }
    var out by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    LaunchedEffect(sess) {
        while (true) {
            out = sess.output()
            delay(500)
        }
    }
    Column(Modifier.padding(16.dp)) {
        Text("Terminal — ${project.name}")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(out.takeLast(20000))
        }
        Row(Modifier.fillMaxWidth()) {
            TextField(input, { input = it }, Modifier.weight(1f), label = { Text("shell") })
            Button(onClick = {
                sess.sendInput(input)
                input = ""
            }) { Text("Run") }
        }
    }
}
