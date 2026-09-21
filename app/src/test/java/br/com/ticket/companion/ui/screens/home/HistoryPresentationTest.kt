package br.com.ticket.companion.ui.screens.home

import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.connection.RetiredConnection
import br.com.ticket.companion.domain.parser.Confidence
import br.com.ticket.companion.domain.parser.Direction
import br.com.ticket.companion.domain.parser.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
