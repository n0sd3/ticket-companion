package br.com.ticket.companion.util

import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import com.google.gson.Gson

object NotificationsExporter {
    fun toJson(events: List<CapturedEventEntity>): String = Gson().toJson(events.map {
        mapOf(
            "eventId" to it.id, "sourceApp" to it.sourceApp, "capturedAt" to it.capturedAt,
            "transactionType" to it.transactionType.name, "direction" to it.direction.name,
            "confidence" to it.confidence.name, "amountCents" to it.amountCents,
            "syncStatus" to it.syncStatus.name, "attempts" to it.attempts,
            "reconciliationStatus" to it.reconciliationStatus, "errorCode" to it.lastErrorCode
        )
    })
}
