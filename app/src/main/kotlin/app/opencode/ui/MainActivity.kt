package app.opencode.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.opencode.agent.Builtins
import app.opencode.tools.ToolRegistry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Home() }
    }
}

@Composable
fun Home() {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("Opencode — Android port (skeleton)")
                Text("Agents: ${Builtins.all.joinToString { it.name }}")
                Text("Tools: ${ToolRegistry.names().size} registered")
                Text("Configure a provider key in Settings (Phase 3).")
            }
        }
    }
}
