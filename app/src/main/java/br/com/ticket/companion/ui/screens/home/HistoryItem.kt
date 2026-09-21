package br.com.ticket.companion.ui.screens.home

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.connection.RetiredConnection

/** Uma linha do histórico, já pronta para exibir. */
@Immutable
data class HistoryItem(
    val id: String,
    val appName: String,
    val appIcon: Drawable?,
    val title: String?,
    val body: String,
    val amount: String?,
    val payer: String?,
    val status: SyncStatus,
    val failure: String?,
    val shortTime: String,
    val fullTime: String,
    val crmResult: String?,
    /** Preenchido quando o evento pertence a outra conexão (empresa anterior). */
    val originNote: String?,
    val canRetry: Boolean
) {
    /** O que aparece em destaque: quem pagou, senão o título, senão o texto. */
    val headline: String get() = payer ?: title ?: body
}

internal fun CapturedEventEntity.toHistoryItem(
    activeConnectionId: String?,
    retiredConnections: List<RetiredConnection>,
    icon: Drawable? = null
): HistoryItem {
    val fromActive = connectionId == activeConnectionId
    val previousCompany = retiredConnections.firstOrNull { it.id == connectionId }?.companyName
    return HistoryItem(
        id = id,
        appName = sourceAppName ?: sourceApp,
        appIcon = icon,
        title = originalTitle,
        body = originalText,
        amount = amountCents?.let(HistoryFormatter::money),
        payer = payerName,
        status = syncStatus,
        failure = lastError,
        shortTime = HistoryFormatter.short(capturedAt),
        fullTime = HistoryFormatter.full(capturedAt),
        crmResult = reconciliationStatus,
        originNote = if (fromActive) null else "De outra conexão (${previousCompany ?: "empresa anterior"})",
        canRetry = fromActive && syncStatus == SyncStatus.SYNC_FAILED
    )
}
