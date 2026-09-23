package br.com.ticket.companion.ui.screens.home

import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.connection.RetiredConnection
import br.com.ticket.companion.domain.parser.Confidence
import br.com.ticket.companion.domain.parser.Direction
import br.com.ticket.companion.domain.parser.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryPresentationTest {
    @Test
    fun `history exposes reconciliation and retired company without allowing cross-connection retry`() {
        val item = event().toHistoryItem(
            activeConnectionId = "new-connection",
            retiredConnections = listOf(RetiredConnection("old-connection", "https://old.example", "Empresa Antiga", "antiga"))
        )

        assertEquals("MATCHED", item.crmResult)
        assertEquals("De outra conexão (Empresa Antiga)", item.originNote)
        assertFalse(item.canRetry)
    }

    @Test
    fun `a captured event says why it was not sent and a delivered one says nothing`() {
        fun reason(type: TransactionType, amount: Long?, status: SyncStatus = SyncStatus.CAPTURED) =
            event().copy(transactionType = type, amountCents = amount, syncStatus = status).toHistoryItem("old-connection", emptyList()).notSentReason

        assertEquals("Não reconheci esta notificação como um recebimento.", reason(TransactionType.UNKNOWN, null))
        assertEquals("Parece um Pix enviado, não um recebimento.", reason(TransactionType.PIX_SENT, 1_000))
        assertEquals("É uma devolução ou estorno, não um recebimento.", reason(TransactionType.PIX_REFUND, 1_000))
        assertEquals("O Pix falhou ou foi cancelado.", reason(TransactionType.PIX_FAILED, 1_000))
        assertEquals("É uma compra no cartão.", reason(TransactionType.PURCHASE, 1_000))
        assertEquals("Recebimento sem um valor único no texto (há mais de um valor?).", reason(TransactionType.TRANSFER_RECEIVED, null))
        assertNull(reason(TransactionType.PIX_RECEIVED, 1_000, SyncStatus.DELIVERED))
        assertNull(reason(TransactionType.PIX_RECEIVED, 1_000, SyncStatus.PENDING_SYNC))
    }

    private fun event() = CapturedEventEntity(
        id = "event", connectionId = "old-connection", fingerprint = "fingerprint",
        sourceApp = "bank.app", sourceAppName = "Banco", originalTitle = "Pix recebido",
        originalText = "Você recebeu um Pix de R$ 10,00", notificationTimestamp = 1_000,
        capturedAt = 1_001, transactionType = TransactionType.PIX_RECEIVED,
        direction = Direction.INCOMING, confidence = Confidence.MEDIUM, amountCents = 1_000,
        parserId = "generic.pt-BR.v1", syncStatus = SyncStatus.DELIVERED,
        reconciliationStatus = "MATCHED"
    )
}
