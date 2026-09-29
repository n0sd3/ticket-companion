package br.com.ticket.companion.service

import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.dedup.NotificationFingerprint
import br.com.ticket.companion.domain.parser.PixClassifier
import javax.inject.Inject

class CapturedEventFactory @Inject constructor(private val classifier: PixClassifier) {
    fun create(connectionId: String?, sourceApp: String, sourceAppName: String?, title: String?, text: String, postTime: Long): CapturedEventEntity? {
        if (connectionId == null) return null
        val parsed = classifier.classify(sourceApp, title, text)
        return CapturedEventEntity(
            connectionId = connectionId,
            fingerprint = NotificationFingerprint.create(sourceApp, title, text, parsed.amountCents, postTime),
            sourceApp = sourceApp,
            sourceAppName = sourceAppName,
            originalTitle = title,
            originalText = text,
            notificationTimestamp = postTime,
            transactionType = parsed.transactionType,
            direction = parsed.direction,
            confidence = parsed.confidence,
            amountCents = parsed.amountCents,
            payerName = parsed.payerName,
            txid = parsed.txid,
            endToEndId = parsed.endToEndId,
            bankReference = parsed.bankReference,
            parserId = parsed.parserId,
            syncStatus = if (parsed.shouldSync) SyncStatus.PENDING_SYNC else SyncStatus.CAPTURED
        )
    }
}
