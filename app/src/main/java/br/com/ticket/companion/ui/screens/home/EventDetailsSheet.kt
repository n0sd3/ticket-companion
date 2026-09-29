package br.com.ticket.companion.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private class Detail(val label: String, val value: String, val tint: Color? = null)

/** Detalhes de um evento capturado, com as ações que fazem sentido para o estado dele. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDetailsSheet(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRetry: (() -> Unit)?,
    onDiscard: (() -> Unit)?
) {
    val error = MaterialTheme.colorScheme.error
    val details = buildList {
        add(Detail("Situação", item.status.label(), item.status.tint()))
        item.notSentReason?.let { add(Detail("Por que não foi enviado", it)) }
        add(Detail("App", item.appName))
        add(Detail("Capturado em", item.fullTime))
        item.payer?.let { add(Detail("Pagador", it)) }
        item.amount?.let { add(Detail("Valor", it)) }
        item.crmResult?.let { add(Detail("Resultado no CRM", it)) }
        item.originNote?.let { add(Detail("Conexão", it)) }
        item.title?.let { add(Detail("Título original", it)) }
        add(Detail("Texto original", item.body))
        item.failure?.let { add(Detail("Erro", it, error)) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Detalhes do lançamento", style = MaterialTheme.typography.titleLarge)
            details.forEach { detail ->
                Column {
                    Text(detail.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(detail.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = detail.tint ?: MaterialTheme.colorScheme.onSurface)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onCopy, Modifier.weight(1f)) { Text("Copiar texto") }
                onRetry?.let { OutlinedButton(it, Modifier.weight(1f)) { Text("Reenviar") } }
            }
            onDiscard?.let { OutlinedButton(it, Modifier.fillMaxWidth()) { Text("Descartar", color = error) } }
        }
    }
}
