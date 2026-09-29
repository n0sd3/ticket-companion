package br.com.ticket.companion.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, onDisconnected: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val uri = LocalUriHandler.current
    var confirmSwitch by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { result -> result?.let(viewModel::export) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    if (confirmSwitch) AlertDialog(
        onDismissRequest = { confirmSwitch = false }, title = { Text("Trocar empresa") },
        text = { Text("A conexão atual será aposentada. Eventos pendentes não serão enviados a outra empresa.") },
        confirmButton = { TextButton({ confirmSwitch = false; viewModel.disconnect(); onDisconnected() }) { Text("Continuar") } },
        dismissButton = { TextButton({ confirmSwitch = false }) { Text("Cancelar") } }
    )
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false }, title = { Text("Limpar histórico") }, text = { Text("Excluir todos os eventos capturados deste aparelho?") },
        confirmButton = { TextButton({ confirmClear = false; viewModel.clearAllData() }) { Text("Excluir") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancelar") } }
    )
    if (state.showAddAppDialog) AlertDialog(
        onDismissRequest = viewModel::hideAddAppDialog, title = { Text("Adicionar app monitorado") },
        text = {
            LazyColumn { items(state.installedApps, key = { it.packageName }) { app -> TextButton({ viewModel.addApp(app) }, Modifier.fillMaxWidth()) { Text(app.displayName) } } }
        }, confirmButton = { TextButton(viewModel::hideAddAppDialog) { Text("Fechar") } }
    )
    if (state.showLogs) AlertDialog(
        onDismissRequest = viewModel::hideLogs, title = { Text("Logs de sincronização") },
        text = { LazyColumn {
            if (state.logs.isEmpty()) item { Text("Nenhum envio registrado") }
            items(state.logs, key = { it.id }) { log ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(log.message)
                    Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(log.at)), style = MaterialTheme.typography.bodySmall)
                }
            }
        } },
        confirmButton = { TextButton(viewModel::hideLogs) { Text("Fechar") } }
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }, navigationIcon = { IconButton(onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Empresa", style = MaterialTheme.typography.titleMedium) }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(state.companyName, style = MaterialTheme.typography.titleMedium); Text(state.serverUrl); Text("Token: ${state.tokenName}")
                    OutlinedButton({ confirmSwitch = true }, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Trocar empresa") }
                } }
            }
            item { Text("Apps monitorados", style = MaterialTheme.typography.titleMedium) }
            items(state.monitoredApps, key = { it.packageName }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(app.displayName); Text(app.packageName, style = MaterialTheme.typography.bodySmall) }
                    Switch(app.isEnabled, { viewModel.toggleApp(app.packageName, it) })
                    TextButton({ viewModel.removeApp(app.packageName) }) { Text("Remover") }
                }
            }
            item { Button(viewModel::showAddAppDialog, Modifier.fillMaxWidth()) { Text("Adicionar app") } }
            item { Preference("Notificar envio", state.notifySyncSuccess, viewModel::setNotifySyncSuccess) }
            item { Preference("Notificar erro", state.notifySyncError, viewModel::setNotifySyncError) }
            if (Build.VERSION.SDK_INT >= 33) item { TextButton({ permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir alertas no Android") } }
            item { Button(viewModel::updateApp, Modifier.fillMaxWidth(), enabled = !state.updateBusy) { Text("Atualizar aplicativo") } }
            state.updateMessage?.let { message -> item { Text(message) } }
            item { OutlinedButton(viewModel::refreshDiagnostics, Modifier.fillMaxWidth()) { Text("Diagnóstico da captura") } }
            items(state.diagnostics) { line -> Text(line, style = MaterialTheme.typography.bodySmall) }
            item { OutlinedButton(viewModel::showLogs, Modifier.fillMaxWidth()) { Text("Logs de sincronização") } }
            item { OutlinedButton({ export.launch("ticket-companion-diagnostico.json") }, Modifier.fillMaxWidth()) { Text("Exportar diagnóstico sem dados pessoais") } }
            state.exportMessage?.let { message -> item { Text(message) } }
            item { OutlinedButton({ confirmClear = true }, Modifier.fillMaxWidth()) { Text("Limpar histórico") } }
            item { TextButton({ uri.openUri("https://github.com/n0sd3/ticket-companion") }, Modifier.fillMaxWidth()) { Text("Código-fonte · versão ${state.appVersion}") } }
        }
    }
}

@Composable
private fun Preference(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label); Switch(checked, onChange) }
}
