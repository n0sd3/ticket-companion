package br.com.ticket.companion.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import br.com.ticket.companion.R

/** Canais do Companion. Canal de notificação só existe no Android 8+; antes disso não há o que criar. */
object AppNotificationChannels {
    const val SYNC = "companion_sync"
    const val RESULTS = "companion_results"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            Triple(SYNC, R.string.notification_channel_sync to R.string.notification_channel_sync_description, NotificationManager.IMPORTANCE_LOW),
            Triple(RESULTS, R.string.notification_channel_sync_results to R.string.notification_channel_sync_results_description, NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { (id, text, importance) ->
            val channel = NotificationChannel(id, context.getString(text.first), importance)
            channel.description = context.getString(text.second)
            manager.createNotificationChannel(channel)
        }
    }
}
