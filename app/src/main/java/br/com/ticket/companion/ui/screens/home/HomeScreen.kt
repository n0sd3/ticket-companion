package br.com.ticket.companion.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import br.com.ticket.companion.data.local.entities.SyncStatus

/** Tela da captura de PIX: situação da conexão, resumo e histórico de eventos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenSettings: () -> Unit, onOpenWeb: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var opened by remember { mutableStateOf<HistoryItem?>(null) }

    LaunchedEffect(owner) { owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refresh() } }

    opened?.let { item ->
        val close = { opened = null }
        EventDetailsSheet(
            item = item,
            onDismiss = close,
            onCopy = { copyToClipboard(context, item.body); toast(context, "Texto copiado") },
            onRetry = if (item.canRetry) ({ viewModel.retryEvent(item.id); close(); toast(context, "Marcado para reenvio") }) else null,
            onDiscard = if (item.status != SyncStatus.DELIVERED) ({ viewModel.discardEvent(item.id); close(); toast(context, "Lançamento descartado") }) else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Captura de PIX") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                        if (state.refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = onOpenWeb) { Icon(Icons.Default.Language, contentDescription = "Abrir atendimento") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Configurações") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "company") { CompanyLine(state.company, state.connection, onReconnect = onOpenSettings) }
            item(key = "summary") { SummaryRow(state.pending, state.deliveredToday, state.lastSync) }
            if (!state.listenerGranted) {
                item(key = "access") { ListenerAccessBanner { context.startActivity(viewModel.listenerSettingsIntent()) } }
            }
            item(key = "apps") { MonitoredAppsStrip(state.apps, onManage = onOpenSettings) }
            historySection(state, onFilter = viewModel::showOnly, onOpen = { opened = it }, onDelete = viewModel::deleteEvent)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("evento_capturado", text))
}

private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
