package br.com.ticket.companion.domain.sync

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class SyncAction { DELIVERED, RECONNECT, FAILED, RETRY }

data class SyncOutcome(val action: SyncAction, val nextAttemptAt: Long? = null) {
    companion object {
        fun fromHttp(status: Int?, attempts: Int, retryAfter: String? = null, now: Long = System.currentTimeMillis()): SyncOutcome {
            if (status == 200) return SyncOutcome(SyncAction.DELIVERED)
            if (status == 401 || status == 403 || status in 300..399) return SyncOutcome(SyncAction.RECONNECT)
            if (status != null && status != 429 && status !in 500..599 || attempts >= 19) return SyncOutcome(SyncAction.FAILED)
            val backoff = minOf(3600000L, 30000L * (1L shl attempts.coerceIn(0, 18)))
            val serverTime = if (status == 429 && retryAfter != null) {
                try {
                    retryAfter.trim().toLongOrNull()?.let { seconds ->
                        if (seconds < 0) null else Math.addExact(now, Math.multiplyExact(seconds, 1000L))
                    } ?: ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
                } catch (_: Exception) { null }
            } else null
            return SyncOutcome(SyncAction.RETRY, maxOf(now + backoff, serverTime ?: 0L))
        }
    }
}
