package br.com.ticket.companion.service

import br.com.ticket.companion.data.local.dao.NotificationDao
import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.data.remote.dto.PixEventRequest
import br.com.ticket.companion.data.remote.dto.PixEventsRequest
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.sync.SyncAction
import br.com.ticket.companion.domain.sync.SyncOutcome
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import br.com.ticket.companion.data.local.entities.DeliveryLogEntity
import br.com.ticket.companion.data.local.entities.LogLevel
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.util.SyncResultNotifier
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncRun { SUCCESS, RETRY, FAILURE }

@Singleton
class EventSyncer @Inject constructor(private val dao: NotificationDao, private val connections: ConnectionManager, private val apiFactory: TicketApiFactory, private val notifier: SyncResultNotifier) {
    // ponytail: one installation has one active connection; serialize sends to preserve local retry state.
    private val mutex = Mutex()
    suspend fun sync(eventId: String? = null, now: Long = System.currentTimeMillis()): SyncRun = mutex.withLock {
        syncPending(eventId, now)
    }

    private suspend fun syncPending(eventId: String?, now: Long): SyncRun {
        val connection = connections.active ?: return SyncRun.SUCCESS
        if (connections.state != br.com.ticket.companion.domain.connection.ConnectionState.CONNECTED) return SyncRun.FAILURE
        val events = if (eventId == null) dao.getPendingSync(connection.id, now, 20)
        else listOfNotNull(dao.getPendingById(eventId, connection.id, now))
        if (events.isEmpty()) return if (dao.countPending(connection.id) > 0) SyncRun.RETRY else SyncRun.SUCCESS
        return try {
            val response = apiFactory.create(connection.serverUrl, connection.accessToken, connections.deviceId)
                .pixEvents(events.first().id, PixEventsRequest(events.map(::toRequest)))
            val outcome = SyncOutcome.fromHttp(response.code(), 0, response.headers()["Retry-After"], now)
            when (outcome.action) {
                SyncAction.DELIVERED -> {
                    val results = response.body()?.results.orEmpty().associateBy { it.eventId }
                    events.forEach { event ->
                        val result = results[event.id]
                        if (result == null || result.status !in setOf("MATCHED", "AMBIGUOUS", "UNMATCHED", "DUPLICATE", "REJECTED", "ERROR")) retry(event, now + 30_000, "INVALID_RESPONSE", "Resposta sem resultado válido para o evento")
                        else if (result.status == "ERROR") dao.markFailed(
                            event.id, event.attempts + 1, "HTTP_${result.statusCode ?: 400}",
                            result.error ?: result.reason ?: "Evento rejeitado"
                        ) else dao.markDelivered(event.id, result.status, result.pixChargeId, result.ticketId, result.reason, now)
                    }
                    if (dao.countPending(connection.id) > 0) SyncRun.RETRY else SyncRun.SUCCESS
                }
                SyncAction.RECONNECT -> {
                    events.forEach { dao.markFailed(it.id, it.attempts + 1, "HTTP_${response.code()}", if (response.code() in 300..399) "Redirect bloqueado" else "Autenticação recusada") }
                    if (connections.active?.id == connection.id) connections.markAuthFailed()
                    SyncRun.FAILURE
                }
                SyncAction.FAILED -> {
                    events.forEach { dao.markFailed(it.id, it.attempts + 1, "HTTP_${response.code()}", if (response.code() == 409) "Conflito de idempotência" else "Evento rejeitado") }
                    if (dao.countPending(connection.id) > 0) SyncRun.RETRY else SyncRun.SUCCESS
                }
                SyncAction.RETRY -> {
                    events.forEach {
                        val retryAt = SyncOutcome.fromHttp(response.code(), it.attempts, response.headers()["Retry-After"], now).nextAttemptAt
                        retry(it, retryAt ?: now, "HTTP_${response.code()}", "Falha temporária")
                    }
                    if (dao.countPending(connection.id) > 0) SyncRun.RETRY else SyncRun.SUCCESS
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            events.forEach { event ->
                val outcome = SyncOutcome.fromHttp(null, event.attempts, now = now)
                retry(event, outcome.nextAttemptAt ?: now + 30_000, "NETWORK", "Falha de conexão")
            }
            if (dao.countPending(connection.id) > 0) SyncRun.RETRY else SyncRun.SUCCESS
        } finally {
            events.forEach { original ->
                dao.getById(original.id)?.let { event ->
                    val success = event.syncStatus == SyncStatus.DELIVERED
                    val level = if (success) LogLevel.SUCCESS else LogLevel.ERROR
                    dao.insertLog(DeliveryLogEntity(level = level, message = "${event.syncStatus.name}: ${event.reconciliationStatus ?: event.lastErrorCode ?: "PENDING"}", eventId = event.id))
                    if (success || event.syncStatus == SyncStatus.SYNC_FAILED) runCatching { notifier.notify(event.id, success) }
                }
            }
            dao.pruneLogs(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1_000)
        }
    }

    private suspend fun retry(event: CapturedEventEntity, nextAttemptAt: Long, code: String, error: String?) {
        if (event.attempts >= 19) dao.markFailed(event.id, event.attempts + 1, code, error)
        else dao.markRetry(event.id, event.attempts + 1, nextAttemptAt, code, error)
    }

    private fun toRequest(event: CapturedEventEntity) = PixEventRequest(
        event.id, connections.deviceId, event.sourceApp, event.sourceAppName,
        Instant.ofEpochMilli(event.notificationTimestamp).toString(), event.transactionType.wireName,
        event.direction.name, checkNotNull(event.amountCents), event.payerName, event.txid,
        event.endToEndId, event.bankReference, event.confidence.name, event.parserId
    )
}
