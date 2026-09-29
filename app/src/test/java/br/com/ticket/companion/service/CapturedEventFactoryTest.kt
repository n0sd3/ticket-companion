package br.com.ticket.companion.service

import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.parser.PixClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CapturedEventFactoryTest {
    private val factory = CapturedEventFactory(PixClassifier())

    @Test
    fun `accepted generic Pix becomes pending on the current connection`() {
        val event = factory.create("connection-a", "bank.app", "Banco", "Pix recebido", "Você recebeu um Pix de R$ 12,34", 60_001)!!

        assertEquals("connection-a", event.connectionId)
        assertEquals(1_234L, event.amountCents)
        assertEquals(SyncStatus.PENDING_SYNC, event.syncStatus)
    }

    @Test
    fun `received transfer without the word Pix becomes pending too`() {
        val event = factory.create("connection-a", "bank.app", "Nubank", "Transferência recebida na conta PJ", "Você recebeu R$ 12,00 de MARIA SOUZA LIMA na conta Nu Empresas.", 60_001)!!

        assertEquals(1_200L, event.amountCents)
        assertEquals(SyncStatus.PENDING_SYNC, event.syncStatus)
    }

    @Test
    fun `outgoing and unknown notifications remain captured and never enter sync`() {
        val event = factory.create("connection-a", "bank.app", "Banco", null, "Você fez um Pix de R$ 12,34", 60_001)!!
        assertEquals(SyncStatus.CAPTURED, event.syncStatus)
    }

    @Test
    fun `a fresh install without any connection does not persist bank text`() {
        assertNull(factory.create(null, "bank.app", "Banco", null, "Você recebeu um Pix de R$ 12,34", 60_001))
    }
}
