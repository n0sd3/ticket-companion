package br.com.ticket.companion.util

import br.com.ticket.companion.service.CapturedEventFactory
import br.com.ticket.companion.domain.parser.PixClassifier
import org.junit.Assert.*
import org.junit.Test

class NotificationsExporterTest {
    @Test fun `diagnostic export excludes banking text payer identifiers and credentials`() {
        val event = CapturedEventFactory(PixClassifier()).create("connection", "bank.app", "Banco", "SEGREDO TITULO", "Pix recebido: R$ 10,00 de Nome Privado", 1_000)!!
            .copy(payerName = "Nome Privado", txid = "secret-txid", lastError = "sensitive-server-message")
        val json = NotificationsExporter.toJson(listOf(event))
        assertTrue(json.contains(event.id))
        assertTrue(json.contains("PENDING_SYNC"))
        listOf("SEGREDO", "Nome Privado", "secret-txid", "sensitive-server-message", "originalText", "payerName").forEach { assertFalse(json.contains(it)) }
    }
}
