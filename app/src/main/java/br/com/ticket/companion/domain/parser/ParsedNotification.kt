package br.com.ticket.companion.domain.parser

enum class TransactionType {
    PIX_RECEIVED, PIX_SENT, PIX_FAILED, PIX_REFUND, TRANSFER_RECEIVED, PURCHASE, UNKNOWN;

    /** Dinheiro entrando na conta: candidato a conciliar uma cobrança Pix. */
    val isCredit: Boolean get() = this == PIX_RECEIVED || this == TRANSFER_RECEIVED

    /**
     * Nome com que o tipo viaja para o CRM, que só concilia `PIX_RECEIVED`. Há bancos (Nubank PJ) que
     * anunciam o Pix recebido como "Transferência recebida", sem escrever "Pix": localmente o registro
     * guarda o que o banco disse, e no fio ele vai como o crédito que é.
     */
    val wireName: String get() = if (isCredit) PIX_RECEIVED.name else name
}
enum class Direction { INCOMING, OUTGOING, UNKNOWN }
enum class Confidence { LOW, MEDIUM, HIGH }

data class ParsedNotification(
    val transactionType: TransactionType = TransactionType.UNKNOWN,
    val direction: Direction = Direction.UNKNOWN,
    val amountCents: Long? = null,
    val payerName: String? = null,
    val txid: String? = null,
    val endToEndId: String? = null,
    val bankReference: String? = null,
    val confidence: Confidence = Confidence.LOW,
    val parserId: String = "generic.pt-BR.v2"
) {
    val shouldSync: Boolean get() = transactionType.isCredit &&
        direction == Direction.INCOMING && (amountCents ?: 0L) > 0L && confidence != Confidence.LOW
}
