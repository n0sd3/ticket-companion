package br.com.ticket.companion.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Um aplicativo do aparelho (em geral um banco) cujas notificações o usuário escolheu acompanhar. */
@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey @ColumnInfo(name = "package_id") val packageId: String,
    val label: String,
    val active: Boolean = true,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
