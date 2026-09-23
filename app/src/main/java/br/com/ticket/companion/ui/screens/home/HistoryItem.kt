package br.com.ticket.companion.ui.screens.home

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.connection.RetiredConnection
import br.com.ticket.companion.domain.parser.TransactionType

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
    val canRetry: Boolean,
    /** Só para eventos que ficaram em "Capturado": por que o app decidiu não enviar. */
    val notSentReason: String? = null
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
        canRetry = fromActive && syncStatus == SyncStatus.SYNC_FAILED,
        notSentReason = notSentReason(syncStatus, transactionType, amountCents)
    )
}

/**
 * Por que um evento ficou em "Capturado": o app não explicava, e o usuário só via "não enviado".
 * Só existe para esse estado — quem foi entregue, está pendente ou falhou já tem a própria situação.
 */
internal fun notSentReason(status: SyncStatus, type: TransactionType, amountCents: Long?): String? {
    if (status != SyncStatus.CAPTURED) return null
    return when (type) {
        TransactionType.PIX_SENT -> "Parece um Pix enviado, não um recebimento."
        TransactionType.PIX_REFUND -> "É uma devolução ou estorno, não um recebimento."
        TransactionType.PIX_FAILED -> "O Pix falhou ou foi cancelado."
        TransactionType.PURCHASE -> "É uma compra no cartão."
        TransactionType.UNKNOWN -> "Não reconheci esta notificação como um recebimento."
        TransactionType.PIX_RECEIVED, TransactionType.TRANSFER_RECEIVED ->
            if ((amountCents ?: 0L) <= 0L) "Recebimento sem um valor único no texto (há mais de um valor?)."
            else "Não elegível para envio."
    }
}
