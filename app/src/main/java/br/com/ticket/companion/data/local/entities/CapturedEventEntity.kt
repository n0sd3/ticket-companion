package br.com.ticket.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.ticket.companion.domain.parser.Confidence
import br.com.ticket.companion.domain.parser.Direction
import br.com.ticket.companion.domain.parser.TransactionType
import java.util.UUID

@Entity(
    tableName = "captured_events",
    indices = [Index(value = ["fingerprint"], unique = true), Index(value = ["sync_status", "connection_id"])]
)
data class CapturedEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "connection_id") val connectionId: String,
    val fingerprint: String,
    @ColumnInfo(name = "source_app") val sourceApp: String,
    @ColumnInfo(name = "source_app_name") val sourceAppName: String?,
    @ColumnInfo(name = "original_title") val originalTitle: String?,
    @ColumnInfo(name = "original_text") val originalText: String,
    @ColumnInfo(name = "notification_timestamp") val notificationTimestamp: Long,
    @ColumnInfo(name = "captured_at") val capturedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "transaction_type") val transactionType: TransactionType,
    val direction: Direction,
    val confidence: Confidence,
    @ColumnInfo(name = "amount_cents") val amountCents: Long?,
    @ColumnInfo(name = "payer_name") val payerName: String? = null,
    val txid: String? = null,
    @ColumnInfo(name = "end_to_end_id") val endToEndId: String? = null,
    @ColumnInfo(name = "bank_reference") val bankReference: String? = null,
    @ColumnInfo(name = "parser_id") val parserId: String,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
    val attempts: Int = 0,
    @ColumnInfo(name = "next_attempt_at") val nextAttemptAt: Long? = null,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String? = null,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "reconciliation_status") val reconciliationStatus: String? = null,
    @ColumnInfo(name = "pix_charge_id") val pixChargeId: Long? = null,
    @ColumnInfo(name = "ticket_id") val ticketId: Long? = null,
    @ColumnInfo(name = "delivered_at") val deliveredAt: Long? = null
)

enum class SyncStatus { CAPTURED, PENDING_SYNC, SYNCING, DELIVERED, SYNC_FAILED, DISCARDED }
