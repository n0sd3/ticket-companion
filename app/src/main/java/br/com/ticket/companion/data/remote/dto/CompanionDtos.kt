package br.com.ticket.companion.data.remote.dto

data class CompanionCompany(val name: String, val slug: String)
data class CompanionMe(val valid: Boolean, val deviceId: String, val tokenName: String, val company: CompanionCompany)

data class PixEventRequest(
    val eventId: String,
    val deviceId: String,
    val sourceApp: String,
    val sourceAppName: String?,
    val notificationTimestamp: String,
    val transactionType: String,
    val direction: String,
    val amountCents: Long,
    val payerName: String?,
    val txid: String?,
    val endToEndId: String?,
    val bankReference: String?,
    val confidence: String,
    val parserId: String
)
data class PixEventsRequest(val events: List<PixEventRequest>)
data class PixEventResult(
    val eventId: String,
    val status: String,
    val pixChargeId: Long? = null,
    val ticketId: Long? = null,
    val amountCents: Long? = null,
    val reason: String? = null,
    val statusCode: Int? = null,
    val error: String? = null
)
data class PixEventsResponse(val results: List<PixEventResult>)
data class HeartbeatRequest(val listenerEnabled: Boolean, val appVersion: String)
