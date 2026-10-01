package com.pixelody.app.modules

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModuleShopActivity : ComponentActivity() {
    private val store by lazy { ModuleStore(this) }
    private var state by mutableStateOf(NotebookState())
    private var status by mutableStateOf("Loading installed modules…")
    private var busy by mutableStateOf(true)
    private fun operation(label: String, work: () -> NotebookState) {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try { state = withContext(Dispatchers.IO) { work() }; status = label }
            catch (e: Exception) { status = "Could not complete this operation. Check the package or device storage. Existing notes were preserved." }
            finally { busy = false }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        busy = false
        operation("Ready. Your notes stay on this device.") { store.read() }
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                var draft by rememberSaveable { mutableStateOf("") }
                var initialized by remember { mutableStateOf(false) }
                var confirmLeave by remember { mutableStateOf(false) }
                var pending by remember { mutableStateOf("") }
                LaunchedEffect(busy) { if (!busy && !initialized) { if (draft.isEmpty()) draft = state.text; initialized = true } }
                val dirty = initialized && draft != state.text
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) operation("Module installed. Open it below.") {
                        val bytes = contentResolver.openInputStream(uri)?.use { it.readBounded(ModulePackage.MAX_BYTES + 1) } ?: error("File unavailable")
                        store.install(bytes)
                    }
                }
                fun action(name: String) {
                    when (name) {
                        "back" -> finish()
                        "remove" -> { draft = state.text; operation("Module removed. Saved notes are kept for reinstall.") { store.remove() } }
                        "import" -> { draft = state.text; picker.launch(arrayOf("*/*")) }
                    }
                }
                BackHandler { if (dirty) { pending = "back"; confirmLeave = true } else finish() }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = { if (dirty) { pending = "back"; confirmLeave = true } else finish() }) { Text("Back to Pixelody") }
                        Text("MODULES", style = MaterialTheme.typography.labelLarge)
                        Text("Your optional tools.", style = MaterialTheme.typography.headlineLarge)
                        Text("Import a downloaded Pixelody module to add a tool. No shop is installed by default.")
                        OutlinedButton(enabled = !busy, onClick = { if (dirty) { pending = "import"; confirmLeave = true } else action("import") }) { Text("Import downloaded module") }
                        if (state.shop != null) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(state.shop!!.getString("name"), style = MaterialTheme.typography.titleLarge)
                                    Text("Connects to the Pixelody website when opened. Test Store purchases only; no real charges. Your music and notebook are not shared.")
                                    Button(enabled = !busy, onClick = { startActivity(android.content.Intent(this@ModuleShopActivity, WebShopActivity::class.java)) }) { Text("Open shop") }
                                    OutlinedButton(enabled = !busy, onClick = { operation("Shop module removed.") { store.removeShop() } }) { Text("Remove shop module") }
                                }
                            }
                        }
                        Text(status)
                        if (state.installed != null) {
                            Text("Your Listening Notes", style = MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(value = draft, onValueChange = { if (it.length <= 20000) draft = it }, enabled = !busy, label = { Text(state.installed!!.getString("prompt")) }, minLines = 6, modifier = Modifier.fillMaxWidth())
                            Text("${draft.length} / 20,000 · ${if (dirty) "Unsaved changes" else "Saved on this device"}")
                            Button(enabled = !busy, onClick = { val text = draft; operation("Saved on this device.") { store.save(text) } }) { Text("Save note") }
                            OutlinedButton(enabled = !busy, onClick = { if (dirty) { pending = "remove"; confirmLeave = true } else action("remove") }) { Text("Remove module") }
                            Text("Removing the module keeps saved notes. Reinstall to return to them.")
                        }
                        if (state.installed != null) Text("A standalone notebook in this build. Album linking, syncing and placement inside a theme come later.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, title = { Text("Discard unsaved edits?") }, text = { Text("Previously saved notes will be kept.") }, confirmButton = { TextButton(onClick = { confirmLeave = false; action(pending) }) { Text("Discard edits") } }, dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep editing") } })
                }
            }
        }
    }
}
