package br.com.ticket.companion.util

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.ticket.companion.R
import br.com.ticket.companion.service.AppNotificationChannels
import br.com.ticket.companion.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncResultNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SecureStorage
) {
    fun notify(eventId: String, success: Boolean) {
        if (success) preferences.lastSyncTime = System.currentTimeMillis()
        if (success && !preferences.notifySyncSuccess || !success && !preferences.notifySyncError) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        AppNotificationChannels.ensure(context)
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(eventId.hashCode(), NotificationCompat.Builder(context, AppNotificationChannels.RESULTS)
            .setSmallIcon(R.drawable.ic_notification_small).setContentTitle("Ticket")
            .setContentText(if (success) "Evento entregue ao Ticket" else "Falha no envio; confira o histórico")
            .setContentIntent(intent).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
    }
}
