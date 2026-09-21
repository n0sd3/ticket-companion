package br.com.ticket.companion.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import br.com.ticket.companion.BuildConfig
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.data.remote.dto.HeartbeatRequest
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.connection.ConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CompanionHeartbeat @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connections: ConnectionManager,
    private val factory: TicketApiFactory
) {
    suspend fun send(): SyncRun {
        val connection = connections.active ?: return SyncRun.SUCCESS
        if (connections.state != ConnectionState.CONNECTED) return SyncRun.FAILURE
        return try {
            val component = ComponentName(context, CaptureNotificationListenerService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.split(':')?.contains(component) == true
            val response = factory.create(connection.serverUrl, connection.accessToken, connections.deviceId)
                .heartbeat(HeartbeatRequest(enabled, BuildConfig.VERSION_NAME))
            when {
                response.isSuccessful -> SyncRun.SUCCESS
                response.code() in 300..399 || response.code() == 401 || response.code() == 403 -> {
                    if (connections.active?.id == connection.id) connections.markAuthFailed()
                    SyncRun.FAILURE
                }
                response.code() == 429 || response.code() >= 500 -> SyncRun.RETRY
                else -> SyncRun.FAILURE
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SyncRun.RETRY
        }
    }
}
