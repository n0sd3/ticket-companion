package br.com.ticket.companion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.ticket.companion.data.local.dao.MonitoredAppDao
import br.com.ticket.companion.data.local.dao.NotificationDao
import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.DeliveryLogEntity
import br.com.ticket.companion.data.local.entities.MonitoredAppEntity

@Database(
    entities = [CapturedEventEntity::class, MonitoredAppEntity::class, DeliveryLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun monitoredAppDao(): MonitoredAppDao

    companion object {
        const val FILE_NAME = "ticket_companion.db"

        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, FILE_NAME).build()
    }
}
