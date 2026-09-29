package br.com.ticket.companion.service

import android.app.Notification
import android.content.ComponentName
import android.os.Build
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
                val text = listOf(
                    extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                    extras.getCharSequence(Notification.EXTRA_TEXT),
                    extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n"),
                    extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
                ).firstNotNullOfOrNull { it?.toString()?.takeIf(String::isNotBlank) } ?: return null
                return Posted(sbn.packageName, extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(), text, sbn.postTime)
            }
        }
    }

    override fun onListenerConnected() {
        CaptureDiagnostics.connected(true)
    }

    override fun onListenerDisconnected() {
        CaptureDiagnostics.connected(false)
        // O sistema desconecta o listener (atualização do app do banco, processo reciclado) e ele só volta se pedirmos.
        Log.w(TAG, "Listener desconectado; pedindo rebind")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) requestRebind(ComponentName(this, CaptureNotificationListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        CaptureDiagnostics.seen(sbn.packageName, sbn.postTime)
        val posted = Posted.from(sbn)
        if (posted == null) {
            CaptureDiagnostics.outcome("descartada: sem texto")
            Log.d(TAG, "Descartada sem texto pacote=${sbn.packageName}")
            return
        }
        scope.launch {
            try {
                ingest(posted)
            } catch (error: Exception) {
                Log.e(TAG, "Falha ao processar ${posted.packageName}", error)
            }
        }
    }

    private suspend fun ingest(posted: Posted) {
        val app = monitoredApps.find(posted.packageName)?.takeIf { it.active }
        if (app == null) { CaptureDiagnostics.outcome("ignorada: app não monitorado"); return }
        // Sem conexão ativa continua capturando, presa à última empresa vinculada (nunca à próxima).
        val connectionId = connections.active?.id ?: connections.lastConnection?.id
        val event = eventFactory.create(connectionId, posted.packageName, app.label, posted.title, posted.text, posted.postedAt)
        if (event == null) { CaptureDiagnostics.outcome("descartada: nenhuma empresa vinculada"); return }
        if (events.insert(event) == -1L) { CaptureDiagnostics.outcome("repetida (já capturada)"); return } // mesma notificação (fingerprint)
        CaptureDiagnostics.outcome("capturada: ${event.transactionType}")

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
