package br.com.ticket.companion.domain.parser

import org.junit.Assert.*
import org.junit.Test

class PixClassifierTest {
    private val classifier = PixClassifier()

    @Test fun `accepts received Pix with positive amount and extracts payer`() {
        val result = classifier.classify("unknown.bank", "Pix recebido", "Você recebeu um Pix de R$ 54,90 de João da Silva às 16:30")
        assertEquals(TransactionType.PIX_RECEIVED, result.transactionType)
        assertEquals(Direction.INCOMING, result.direction)
        assertEquals(5490L, result.amountCents)
        assertEquals("João da Silva", result.payerName)
        assertEquals(Confidence.MEDIUM, result.confidence)
        assertTrue(result.shouldSync)
    }

    @Test fun `negative evidence wins even with received title`() {
        mapOf(
            "Você fez um Pix de R$ 50,00" to TransactionType.PIX_SENT,
            "Você enviou R$ 50,00 via Pix" to TransactionType.PIX_SENT,
            "Pix de R$ 50,00 agendado" to TransactionType.UNKNOWN,
            "Pix de R$ 50,00 pendente" to TransactionType.UNKNOWN,
            "Pix de R$ 50,00 em processamento" to TransactionType.UNKNOWN,
            "Pix de R$ 50,00 falhou" to TransactionType.PIX_FAILED,
            "Pix de R$ 50,00 cancelado" to TransactionType.PIX_FAILED,
            "Você recebeu uma devolução de R$ 50,00" to TransactionType.PIX_REFUND,
            "Pix de R$ 50,00 devolvido" to TransactionType.PIX_REFUND,
            "MED Pix R$ 50,00" to TransactionType.PIX_REFUND
        ).forEach { (text, type) ->
            val result = classifier.classify("com.nu.production", "Pix recebido", text)
            assertEquals(text, type, result.transactionType)
            assertFalse(text, result.shouldSync)
        }
    }

    // Forma real de uma notificação do Nubank PJ (só o nome foi trocado): o banco anuncia o Pix
    // recebido como "Transferência recebida" e nunca escreve a palavra "Pix".
    @Test fun `bank that announces a received transfer without saying Pix is accepted as a medium confidence credit`() {
        val result = classifier.classify(
            "com.nu.production",
            "Transferência recebida na conta PJ",
            "Você recebeu R$ 12,00 de MARIA SOUZA LIMA na conta Nu Empresas."
        )
        assertEquals(TransactionType.TRANSFER_RECEIVED, result.transactionType)
        assertEquals(Direction.INCOMING, result.direction)
        assertEquals(1200L, result.amountCents)
        assertEquals("MARIA SOUZA LIMA", result.payerName)
        assertEquals(Confidence.MEDIUM, result.confidence)
        assertTrue(result.shouldSync)
    }

    @Test fun `received transfer wording counts as a credit too`() {
        listOf(
            "Você recebeu uma transferência de R$ 50,00",
            "Transferência recebida: R$ 50,00 de Maria Silva"
        ).forEach {
            val result = classifier.classify("unknown", null, it)
            assertEquals(it, TransactionType.TRANSFER_RECEIVED, result.transactionType)
            assertTrue(it, result.shouldSync)
        }
    }

    @Test fun `received transfer with negative evidence or without one clear amount never syncs`() {
        listOf(
            "Transferência recebida: devolução de R$ 12,00",
            "Transferência recebida estornada de R$ 12,00",
            "Transferência recebida pendente de R$ 12,00",
            "Você não recebeu uma transferência de R$ 12,00",
            "Transferência recebida de R$ 12,00. Saldo R$ 100,00",
            "Transferência recebida",
            "Você enviou uma transferência de R$ 12,00",
            "Você recebeu R$ 12,00 de cashback"
        ).forEach { assertFalse(it, classifier.classify("unknown", null, it).shouldSync) }
    }

    @Test fun `payer name stops before the account suffix`() {
        val result = classifier.classify("unknown", "Transferência recebida", "Você recebeu R$ 12,00 de João da Silva na conta Nu Empresas.")
        assertEquals("João da Silva", result.payerName)
    }

    @Test fun `does not synchronize purchases missing or invalid amounts`() {
        listOf(
            "Compra no cartão de R$ 50,00",
            "Você recebeu um Pix",
            "Você recebeu um Pix de R$ 0,00",
            "Você recebeu um Pix de R$ -50,00",
            "Você recebeu um Pix de R$ 50,001",
            "Você recebeu um Pix de R$ 50,00 e R$ 60,00",
            "Você receberá um Pix de R$ 50,00"
        ).forEach { assertFalse(it, classifier.classify("unknown", null, it).shouldSync) }
    }

    @Test fun `does not invent unlabeled payment identifiers`() {
        val plain = classifier.classify("unknown", "Pix recebido", "R$ 50,00 ABC123")
        assertNull(plain.txid)
        assertNull(plain.bankReference)
        val labeled = classifier.classify("unknown", "Pix recebido", "R$ 50,00 txid: ABC123 referência: BANK456 E1234567820260919163012345678901")
        assertEquals("ABC123", labeled.txid)
        assertEquals("BANK456", labeled.bankReference)
        assertEquals("E1234567820260919163012345678901", labeled.endToEndId)
    }

    @Test fun `negated receipt and instructions cannot trigger a payment confirmation`() {
        listOf(
            "Você não recebeu um Pix de R$ 50,00",
            "Confirme se você recebeu um Pix de R$ 50,00",
            "Aguardando Pix recebido de R$ 50,00"
        ).forEach { assertFalse(it, classifier.classify("unknown", null, it).shouldSync) }
    }

    @Test fun `sender attribution is not an outgoing payment`() {
        val result = classifier.classify("unknown", "Pix recebido", "R$ 50,00 enviado por Maria Silva.")
        assertTrue(result.shouldSync)
        assertEquals("Maria Silva", result.payerName)
    }

    @Test fun `amount followed by sentence punctuation is still read exactly`() {
        mapOf(
            "Você recebeu um Pix de R$ 54,90." to 5490L,
            "Você recebeu um Pix de R$ 54,90, enviado por Maria Silva" to 5490L,
            "Você recebeu um Pix de R$ 1.234,56." to 123456L
        ).forEach { (text, cents) ->
            val result = classifier.classify("unknown", "Pix recebido", text)
            assertEquals(text, cents, result.amountCents)
            assertTrue(text, result.shouldSync)
        }
    }

    @Test fun `same amount repeated in title and body is not ambiguous but different amounts are`() {
        val repeated = classifier.classify("unknown", "Pix recebido de R$ 54,90", "João da Silva te enviou R$ 54,90")
        assertEquals(5490L, repeated.amountCents)
        assertTrue(repeated.shouldSync)
        val different = classifier.classify("unknown", "Pix recebido de R$ 54,90", "Saldo atual R$ 1.000,00")
        assertNull(different.amountCents)
        assertFalse(different.shouldSync)
        val malformedSibling = classifier.classify("unknown", "Pix recebido de R$ 50,00", "Detalhe R$ 50,001")
        assertNull(malformedSibling.amountCents)
        assertFalse(malformedSibling.shouldSync)
    }
}
