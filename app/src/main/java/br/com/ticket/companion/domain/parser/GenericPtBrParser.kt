package br.com.ticket.companion.domain.parser

import java.text.Normalizer
import java.util.Locale

object GenericPtBrParser {
    private val money = Regex("(?:R\\$|RS)\\s*(-?[0-9]+(?:[.,][0-9]+)*)|(-?[0-9]+(?:[.,][0-9]+)*)\\s+reais\\b", RegexOption.IGNORE_CASE)
    private val payer = Regex("(?:\\bde|\\benviado por)\\s+([\\p{L}][\\p{L} '\\-]*?)(?=[.,\\n]|\\s+(?:às|em|via|na\\s+conta)\\b|\\s+\\*|$)", RegexOption.IGNORE_CASE)

    fun parse(text: String): ParsedNotification {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)
        fun has(pattern: String) = Regex(pattern).containsMatchIn(normalized)
        val amounts = money.findAll(text.replace('\u00a0', ' ').replace('\u202f', ' '))
            .map { MoneyParser.toCents(it.groupValues[1].ifEmpty { it.groupValues[2] }) }.toList()
        val amount = amounts.distinct().singleOrNull()
        val isPix = has("\\bpix\\b")
        // Nubank PJ avisa "Transferência recebida na conta PJ" e nunca escreve "Pix": o banco chama o
        // crédito de transferência. Só vale com essa âncora — "Você recebeu R$ 5 de cashback" não é crédito.
        val transferCredit = has("transferencia recebida|recebeu uma transferencia")
        val related = isPix || transferCredit // os sinais negativos valem para os dois jeitos de anunciar
        val type = when {
            related && has("nao foi possivel|falhou|cancelad|recusad|estornad") -> TransactionType.PIX_FAILED
            related && has("devolu|devolvid|estorno|\\bmed\\b") -> TransactionType.PIX_REFUND
            related && has("agendad|pendente|em processamento|aguardando|nao recebeu|confirme se") -> TransactionType.UNKNOWN
            related && has("voce fez|voce enviou|enviado(?! por\\b)|pagou|pagamento realizado|transferiu") -> TransactionType.PIX_SENT
            isPix && has("recebeu (um )?pix|pix recebido|recebimento de pix|transferencia recebida via pix") && amount != null -> TransactionType.PIX_RECEIVED
            transferCredit -> TransactionType.TRANSFER_RECEIVED
            has("compra.*cartao") -> TransactionType.PURCHASE
            else -> TransactionType.UNKNOWN
        }
        val direction = when (type) {
            TransactionType.PIX_RECEIVED, TransactionType.TRANSFER_RECEIVED -> Direction.INCOMING
            TransactionType.PIX_SENT, TransactionType.PURCHASE -> Direction.OUTGOING
            else -> Direction.UNKNOWN
        }
        fun identifier(label: String, length: Int) = Regex("\\b(?:$label):\\s*([A-Za-z0-9]{1,$length})(?![A-Za-z0-9])", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)
        return ParsedNotification(
            transactionType = type,
            direction = direction,
            amountCents = amount,
            payerName = payer.find(text)?.groupValues?.get(1)?.trim()?.take(80),
            txid = identifier("txid|identificador", 35),
            endToEndId = Regex("\\bE[0-9]{8}[0-9]{12}[A-Za-z0-9]{11}\\b").find(text)?.value,
            bankReference = identifier("referência|referencia|bankReference", 80),
            confidence = if (type.isCredit && (amount ?: 0) > 0) Confidence.MEDIUM else Confidence.LOW
        )
    }
}
