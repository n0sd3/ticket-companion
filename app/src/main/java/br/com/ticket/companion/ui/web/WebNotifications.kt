package br.com.ticket.companion.ui.web

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.ticket.companion.R
import br.com.ticket.companion.domain.web.WebNotificationText

/** Notificações do atendimento web. Roda no processo `:web`: não usa Hilt, Room nem as preferências do app. */
internal object WebNotifications {
    const val BRIDGE_NAME = "TicketAndroidNotifications"
    const val SERVICE_NOTIFICATION_ID = 4010
    private const val SERVICE_CHANNEL_ID = "web_background"
    private const val MESSAGE_CHANNEL_ID = "web_messages"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(SERVICE_CHANNEL_ID, "Atendimento em segundo plano", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Mantém o atendimento conectado enquanto o app está aberto." }
        )
        manager.createNotificationChannel(
            NotificationChannel(MESSAGE_CHANNEL_ID, "Mensagens do atendimento", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Novos tickets e mensagens." }
        )
    }

    fun serviceNotification(context: Context, origin: String?): Notification =
        NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("Atendimento ativo")
            .setContentText("Mantendo o atendimento conectado em segundo plano.")
            .setContentIntent(openIntent(context, origin))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun show(context: Context, origin: String, title: String?, body: String?, tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels(context)
        val text = WebNotificationText.clean(body, "Nova notificação", WebNotificationText.MAX_BODY)
        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(WebNotificationText.clean(title, "Ticket", WebNotificationText.MAX_TITLE))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent(context, origin))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        NotificationManagerCompat.from(context).notify(WebNotificationText.idFor(tag, System.currentTimeMillis()), notification)
    }

    private fun openIntent(context: Context, origin: String?): PendingIntent {
        val intent = (if (origin != null) WebActivity.intent(context, origin) else Intent(context, br.com.ticket.companion.ui.MainActivity::class.java))
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Troca `window.Notification` pela ponte nativa. Só é injetado na origem travada. */
    val BRIDGE_SCRIPT: String = """
        (function(){
          if (window.__ticketAndroidNotificationsInstalled) { return; }
          window.__ticketAndroidNotificationsInstalled = true;
          function send(title, body, tag) {
            try { $BRIDGE_NAME.post(String(title || 'Ticket'), String(body || ''), String(tag || Date.now())); } catch (e) {}
          }
          function AndroidNotification(title, options) {
            options = options || {};
            this.title = String(title || 'Ticket');
            this.body = String(options.body || '');
            this.tag = String(options.tag || Date.now());
            send(this.title, this.body, this.tag);
          }
          AndroidNotification.prototype.close = function() {};
          AndroidNotification.permission = 'granted';
          AndroidNotification.requestPermission = function(callback) {
            if (typeof callback === 'function') { callback('granted'); }
            return Promise.resolve('granted');
          };
          window.Notification = AndroidNotification;
        })();
    """.trimIndent()
}
