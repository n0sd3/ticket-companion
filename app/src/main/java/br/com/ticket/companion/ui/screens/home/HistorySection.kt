package br.com.ticket.companion.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.ui.theme.StatusColors

/** Como cada estado de envio aparece para o usuário. */
internal fun SyncStatus.label(): String = when (this) {
    SyncStatus.DELIVERED -> "Entregue ao CRM"
    SyncStatus.SYNC_FAILED -> "Falha no envio"
    SyncStatus.SYNCING -> "Enviando"
    SyncStatus.DISCARDED -> "Descartado"
    SyncStatus.PENDING_SYNC -> "Pendente"
    SyncStatus.CAPTURED -> "Capturado (não enviado)"
}

@Composable
internal fun SyncStatus.tint(): Color = when (this) {
    SyncStatus.DELIVERED -> StatusColors.delivered
    SyncStatus.SYNC_FAILED -> MaterialTheme.colorScheme.error
    SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun SyncStatus.symbol(): ImageVector = when (this) {
    SyncStatus.DELIVERED -> Icons.Default.CheckCircle
    SyncStatus.SYNC_FAILED -> Icons.Default.Error
    SyncStatus.DISCARDED -> Icons.Default.Delete
    else -> Icons.Default.Schedule
}

/** Abas de filtro + a lista (carregando, vazia ou com linhas) como itens da `LazyColumn` da Home. */
internal fun LazyListScope.historySection(
    state: HomeUiState,
    onFilter: (HistoryFilter) -> Unit,
    onOpen: (HistoryItem) -> Unit,
    onDelete: (String) -> Unit
) {
    item(key = "tabs") {
        val filters = HistoryFilter.entries
        TabRow(selectedTabIndex = filters.indexOf(state.filter)) {
            filters.forEach { filter ->
                Tab(
                    selected = filter == state.filter,
                    onClick = { onFilter(filter) },
                    text = { Text(if (filter == HistoryFilter.DELIVERED) "Entregues" else "Em aberto") }
                )
            }
        }
    }
    when {
        state.loadingHistory -> item(key = "loading") { Placeholder { CircularProgressIndicator() } }
        state.history.isEmpty() -> item(key = "empty") {
            Placeholder { Text("Nenhuma notificação capturada", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        else -> items(state.history, key = { it.id }) { item ->
            HistoryRow(item, onClick = { onOpen(item) }, onDelete = { onDelete(item.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun Placeholder(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(onClick = onClick) {
        ListItem(
            leadingContent = { item.appIcon?.let { AppIcon(it, null, 36) } },
            overlineContent = { Text("${item.appName} · ${item.shortTime}") },
            headlineContent = { Text(item.headline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item.amount?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
                    StatusPill(item.status)
                    item.crmResult?.let { Text("CRM: $it", style = MaterialTheme.typography.labelSmall) }
                }
            },
            trailingContent = {
                IconButton(onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(20.dp)) }
            }
        )
        item.originNote?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun StatusPill(status: SyncStatus) {
    val tint = status.tint()
    Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.12f), contentColor = tint) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(status.symbol(), contentDescription = null, modifier = Modifier.size(12.dp))
            Text(status.label(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
