package br.com.ticket.companion.ui.screens.home

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticket.companion.data.local.dao.MonitoredAppDao
import br.com.ticket.companion.data.local.dao.NotificationDao
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.connection.ConnectionState
import br.com.ticket.companion.service.SyncWorker
import br.com.ticket.companion.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Quais eventos a lista mostra: os já entregues ao CRM ou os que ainda dependem de envio. */
enum class HistoryFilter { DELIVERED, OPEN }

data class MonitoredAppBadge(val packageId: String, val label: String, val icon: Drawable?)

data class HomeUiState(
    val company: String? = null,
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val pending: Int = 0,
    val deliveredToday: Int = 0,
    val lastSync: String? = null,
    val listenerGranted: Boolean = false,
    val apps: List<MonitoredAppBadge> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.DELIVERED,
    val history: List<HistoryItem> = emptyList(),
    val loadingHistory: Boolean = true,
    val refreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val events: NotificationDao,
    private val monitoredApps: MonitoredAppDao,
    private val storage: SecureStorage,
    private val connections: ConnectionManager
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state.asStateFlow()
    private val iconCache = HashMap<String, Drawable?>()

    init {
        refresh()
        viewModelScope.launch { events.getAllFlow().collect { reloadSummary(); reloadHistory() } }
    }

    fun refresh() {
        state.update { it.copy(refreshing = true, listenerGranted = listenerGranted()) }
        viewModelScope.launch {
            reloadSummary()
            reloadHistory()
            SyncWorker.enqueue(context)
            state.update { it.copy(refreshing = false) }
        }
    }

    fun checkListenerAccess() = state.update { it.copy(listenerGranted = listenerGranted()) }

    fun listenerSettingsIntent() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun showOnly(filter: HistoryFilter) {
        state.update { it.copy(filter = filter) }
        viewModelScope.launch { reloadHistory() }
    }

    fun deleteEvent(id: String) = viewModelScope.launch { events.delete(id); refresh() }

    fun retryEvent(id: String) = viewModelScope.launch {
        connections.active?.let { events.retry(id, it.id) }
        SyncWorker.enqueue(context)
        refresh()
    }

    fun discardEvent(id: String) = viewModelScope.launch { events.updateStatus(id, SyncStatus.DISCARDED); refresh() }

    private fun listenerGranted() = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    private suspend fun reloadSummary() {
        val active = connections.active
        val apps = monitoredApps.activeOnes()
        val badges = withContext(Dispatchers.IO) { apps.map { MonitoredAppBadge(it.packageId, it.label, iconOf(it.packageId)) } }
        val since = HistoryFormatter.startOfDay(System.currentTimeMillis())
        state.update {
            it.copy(
                company = active?.companyName,
                connection = connections.state,
                pending = active?.let { connection -> events.countPending(connection.id) } ?: 0,
                deliveredToday = events.countDeliveredSince(since),
                lastSync = storage.lastSyncTime.takeIf { time -> time > 0 }?.let(HistoryFormatter::full),
                apps = badges
            )
        }
    }

    private suspend fun reloadHistory() {
        state.update { it.copy(loadingHistory = true) }
        val filter = state.value.filter
        val active = connections.active?.id
        val retired = connections.retiredConnections
        val shown = events.getRecent().filter { event ->
            when (filter) {
                HistoryFilter.DELIVERED -> event.syncStatus == SyncStatus.DELIVERED
                HistoryFilter.OPEN -> event.syncStatus != SyncStatus.DELIVERED && event.syncStatus != SyncStatus.DISCARDED
            }
        }
        val items = withContext(Dispatchers.IO) { shown.map { it.toHistoryItem(active, retired, iconOf(it.sourceApp)) } }
        state.update { it.copy(history = items, loadingHistory = false) }
    }

    private fun iconOf(packageId: String): Drawable? =
        iconCache.getOrPut(packageId) { runCatching { context.packageManager.getApplicationIcon(packageId) }.getOrNull() }
}
