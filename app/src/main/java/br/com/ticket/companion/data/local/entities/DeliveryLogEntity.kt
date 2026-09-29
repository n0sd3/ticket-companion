package br.com.ticket.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class LogLevel { INFO, SUCCESS, ERROR, WARNING }

/** Linha do registro de envios. Nunca guarda texto bancário, pagador nem credenciais. */
@Entity(tableName = "delivery_logs")
data class DeliveryLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val at: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String,
    @ColumnInfo(name = "event_id") val eventId: String? = null
)
