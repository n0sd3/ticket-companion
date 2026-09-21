package br.com.ticket.companion.domain.parser

enum class TransactionType { PIX_RECEIVED, PIX_SENT, PIX_FAILED, PIX_REFUND, TRANSFER_RECEIVED, PURCHASE, UNKNOWN }
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
    val parserId: String = "generic.pt-BR.v1"
) {
    val shouldSync: Boolean get() = transactionType == TransactionType.PIX_RECEIVED &&
        direction == Direction.INCOMING && (amountCents ?: 0L) > 0L && confidence != Confidence.LOW
}
