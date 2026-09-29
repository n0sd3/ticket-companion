package br.com.ticket.companion.ui.web

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import br.com.ticket.companion.BuildConfig
import br.com.ticket.companion.domain.web.WebOrigin

/**
 * Mantém o processo `:web` vivo enquanto o atendimento está aberto em segundo plano.
 * Não reinicia sozinho: sem a WebView não há conexão para manter (START_NOT_STICKY), e o Android 15
 * limita serviços `dataSync` (onTimeout).
 */
class TicketWebService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val origin = WebOrigin.normalize(intent?.getStringExtra(WebActivity.EXTRA_ORIGIN), BuildConfig.DEBUG)
        WebNotifications.ensureChannels(this)
        try {
            val notification = WebNotifications.serviceNotification(this, origin)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(this, WebNotifications.SERVICE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(WebNotifications.SERVICE_NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (origin == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) = stopSelf()

    override fun onBind(intent: Intent?): IBinder? = null
}
