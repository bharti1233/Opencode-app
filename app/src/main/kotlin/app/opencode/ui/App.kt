package app.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.opencode.workspace.Project

sealed interface Screen {
    data object Projects : Screen
    data class Chat(val project: Project) : Screen
    data class Term(val project: Project) : Screen
    data object Settings : Screen
}

@Composable
fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Projects) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(8.dp)) {
                Row {
                    Button(onClick = { screen = Screen.Projects }) { Text("Projects") }
                    val chat = screen as? Screen.Chat
                    val term = screen as? Screen.Term
                    val proj = chat?.project ?: term?.project
                    if (proj != null) {
                        Button(onClick = { screen = Screen.Chat(proj) }) { Text("Chat") }
                        Button(onClick = { screen = Screen.Term(proj) }) { Text("Term") }
                    }
                    Button(onClick = { screen = Screen.Settings }) { Text("Settings") }
                }
                when (val s = screen) {
                    is Screen.Projects -> ProjectsScreen { screen = Screen.Chat(it) }
                    is Screen.Chat -> ChatScreen(s.project)
                    is Screen.Term -> TerminalScreen(s.project)
                    is Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}
