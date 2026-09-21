package br.com.ticket.companion.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import br.com.ticket.companion.data.local.dao.MonitoredAppDao
import br.com.ticket.companion.data.local.dao.NotificationDao
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.connection.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CaptureNotificationListenerService : NotificationListenerService() {
    @Inject lateinit var events: NotificationDao
    @Inject lateinit var monitoredApps: MonitoredAppDao
    @Inject lateinit var connections: ConnectionManager
    @Inject lateinit var eventFactory: CapturedEventFactory
    @Inject lateinit var syncer: EventSyncer
    @Inject lateinit var heartbeat: CompanionHeartbeat

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** O que interessa de uma notificação, já tirado do `Bundle` de extras. */
    private class Posted(val packageName: String, val title: String?, val text: String, val postedAt: Long) {
        companion object {
            fun from(sbn: StatusBarNotification): Posted? {
                val extras = sbn.notification.extras
                val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                val text = body?.toString()?.takeIf { it.isNotBlank() } ?: return null
                return Posted(sbn.packageName, extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(), text, sbn.postTime)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val posted = Posted.from(sbn) ?: return
        scope.launch {
            try {
                ingest(posted)
            } catch (error: Exception) {
                Log.e(TAG, "Falha ao processar ${posted.packageName}", error)
            }
        }
    }

    private suspend fun ingest(posted: Posted) {
        val app = monitoredApps.find(posted.packageName)?.takeIf { it.active } ?: return
        // Sem conexão ativa continua capturando, presa à última empresa vinculada (nunca à próxima).
        val connectionId = connections.active?.id ?: connections.lastConnection?.id
        val event = eventFactory.create(connectionId, posted.packageName, app.label, posted.title, posted.text, posted.postedAt) ?: return
        if (events.insert(event) == -1L) return // mesma notificação já capturada (fingerprint)

        scope.launch { heartbeat.send() }
        Log.d(TAG, "Capturado pacote=${posted.packageName} tipo=${event.transactionType}")
        if (event.syncStatus != SyncStatus.PENDING_SYNC) return
        SyncWorker.enqueue(applicationContext, expedited = true)
        syncer.sync(event.id)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "TicketCompanion"
    }
}
