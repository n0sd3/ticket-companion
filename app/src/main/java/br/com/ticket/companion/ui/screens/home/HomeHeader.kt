package br.com.ticket.companion.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import br.com.ticket.companion.domain.connection.ConnectionState
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
internal fun CompanyLine(company: String?, connection: ConnectionState, onReconnect: () -> Unit) {
    Column {
        Text(company ?: "Sem conexão ativa", style = MaterialTheme.typography.titleMedium)
        if (connection != ConnectionState.CONNECTED) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reconecte para retomar os envios.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onReconnect) { Text("Reconectar") }
            }
        }
    }
}

@Composable
internal fun SummaryRow(pending: Int, deliveredToday: Int, lastSync: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountTile("Aguardando envio", pending, Modifier.weight(1f))
            CountTile("Entregues hoje", deliveredToday, Modifier.weight(1f))
        }
        Text(
            "Último envio: ${lastSync ?: "nunca"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountTile(label: String, value: Int, modifier: Modifier) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ListenerAccessBanner(onEnable: () -> Unit) {
    Surface(
        onClick = onEnable,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Notifications, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text("Falta liberar o acesso às notificações", style = MaterialTheme.typography.titleSmall)
                Text("Toque para permitir que o app leia as notificações dos bancos escolhidos.", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
internal fun MonitoredAppsStrip(apps: List<MonitoredAppBadge>, onManage: () -> Unit) {
    val empty = apps.isEmpty()
    OutlinedCard(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Apps monitorados", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (empty) "Nenhum app escolhido. Toque para selecionar." else "${apps.size} em acompanhamento",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (empty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                apps.take(MAX_ICONS).forEach { app -> app.icon?.let { AppIcon(it, app.label, 28) } }
                if (apps.size > MAX_ICONS) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Text("+${apps.size - MAX_ICONS}", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
internal fun AppIcon(icon: android.graphics.drawable.Drawable, description: String?, size: Int) {
    Image(
        painter = rememberDrawablePainter(drawable = icon),
        contentDescription = description,
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(6.dp))
    )
}

private const val MAX_ICONS = 5
