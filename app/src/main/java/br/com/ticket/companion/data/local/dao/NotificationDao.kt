package br.com.ticket.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.data.local.entities.DeliveryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insertLog(log: DeliveryLogEntity)

    @Query("SELECT * FROM delivery_logs ORDER BY at DESC LIMIT 100")
    suspend fun getRecentLogs(): List<DeliveryLogEntity>

    @Query("DELETE FROM delivery_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM delivery_logs WHERE at < :before")
    suspend fun pruneLogs(before: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: CapturedEventEntity): Long

    @Query("SELECT * FROM captured_events ORDER BY captured_at DESC")
    fun getAllFlow(): Flow<List<CapturedEventEntity>>

    @Query("SELECT * FROM captured_events ORDER BY captured_at DESC")
    suspend fun getAll(): List<CapturedEventEntity>

    @Query("SELECT * FROM captured_events ORDER BY captured_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<CapturedEventEntity>

    @Query("SELECT * FROM captured_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CapturedEventEntity?

    @Query("SELECT * FROM captured_events WHERE connection_id = :connectionId AND sync_status = 'PENDING_SYNC' AND (next_attempt_at IS NULL OR next_attempt_at <= :now) ORDER BY captured_at ASC LIMIT :limit")
    suspend fun getPendingSync(connectionId: String, now: Long, limit: Int = 20): List<CapturedEventEntity>

    @Query("SELECT * FROM captured_events WHERE id = :id AND connection_id = :connectionId AND sync_status = 'PENDING_SYNC' AND (next_attempt_at IS NULL OR next_attempt_at <= :now) LIMIT 1")
    suspend fun getPendingById(id: String, connectionId: String, now: Long): CapturedEventEntity?

    @Query("SELECT COUNT(*) FROM captured_events WHERE connection_id = :connectionId AND sync_status = 'PENDING_SYNC'")
    suspend fun countPending(connectionId: String): Int

    @Query("SELECT COUNT(*) FROM captured_events WHERE sync_status = 'DELIVERED' AND delivered_at >= :since")
    suspend fun countDeliveredSince(since: Long): Int

    @Query("UPDATE captured_events SET sync_status = 'SYNCING' WHERE id IN (:ids) AND sync_status = 'PENDING_SYNC'")
    suspend fun markSyncing(ids: List<String>)

    @Query("UPDATE captured_events SET sync_status = 'PENDING_SYNC', attempts = :attempts, next_attempt_at = :nextAttemptAt, last_error_code = :errorCode, last_error = :error WHERE id = :id")
    suspend fun markRetry(id: String, attempts: Int, nextAttemptAt: Long, errorCode: String?, error: String?)

    @Query("UPDATE captured_events SET sync_status = 'DELIVERED', reconciliation_status = :reconciliationStatus, pix_charge_id = :pixChargeId, ticket_id = :ticketId, last_error = :reason, last_error_code = NULL, next_attempt_at = NULL, delivered_at = :deliveredAt WHERE id = :id")
    suspend fun markDelivered(id: String, reconciliationStatus: String, pixChargeId: Long?, ticketId: Long?, reason: String?, deliveredAt: Long)

    @Query("UPDATE captured_events SET sync_status = 'SYNC_FAILED', attempts = :attempts, last_error_code = :errorCode, last_error = :error, next_attempt_at = NULL WHERE id = :id")
    suspend fun markFailed(id: String, attempts: Int, errorCode: String?, error: String?)

    @Query("UPDATE captured_events SET sync_status = 'PENDING_SYNC', attempts = 0, next_attempt_at = NULL, last_error_code = NULL, last_error = NULL WHERE id = :id AND connection_id = :connectionId")
    suspend fun retry(id: String, connectionId: String): Int

    @Query("UPDATE captured_events SET connection_id = :newConnectionId, sync_status = 'PENDING_SYNC', attempts = 0, next_attempt_at = NULL, last_error_code = NULL, last_error = NULL WHERE connection_id IN (:retiredIds) AND sync_status IN ('PENDING_SYNC', 'SYNC_FAILED')")
    suspend fun reassignPending(retiredIds: List<String>, newConnectionId: String): Int

    @Query("UPDATE captured_events SET sync_status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM captured_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM captured_events")
    suspend fun deleteAll()
}
