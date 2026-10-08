package app.opencode.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.opencode.agent.Builtins
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionRequest
import app.opencode.permissions.PermissionSession
import app.opencode.permissions.RuleStore
import app.opencode.tools.ToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Home() }
    }
}

@Composable
fun Home() {
    val ctx = LocalContext.current
    val store = remember { RuleStore(ctx) }
    val session = remember { PermissionSession(store.load().toMutableList()) }
    var pending by remember {
        mutableStateOf<Pair<PermissionRequest, CompletableDeferred<Decision>>?>(null)
    }
    var lastResult by remember { mutableStateOf("no asks yet") }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("Opencode — Android port (skeleton)")
                Text("Agents: ${Builtins.all.joinToString { it.name }}")
                Text("Tools: ${ToolRegistry.names().size} registered")
                Text("Configure a provider key in Settings (Phase 3).")
                Button(onClick = {
                    scope.launch {
                        val ok = session.ask("bash", "./gradlew assembleDebug") { req ->
                            val d = CompletableDeferred<Decision>()
                            pending = req to d
                            val dec = d.await()
                            pending = null
                            dec
                        }
                        store.save(session.rules)
                        lastResult = "ask result: $ok"
                    }
                }) { Text("Test permission ask") }
                Text(lastResult)
            }
        }
    }
    pending?.let { (req, deferred) ->
        PermissionDialog(req) { deferred.complete(it) }
    }
}
