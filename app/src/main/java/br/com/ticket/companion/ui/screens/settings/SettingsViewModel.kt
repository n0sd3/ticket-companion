package br.com.ticket.companion.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticket.companion.data.local.dao.MonitoredAppDao
import br.com.ticket.companion.data.local.dao.NotificationDao
import br.com.ticket.companion.data.local.entities.MonitoredAppEntity
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.net.Uri
import br.com.ticket.companion.data.local.entities.DeliveryLogEntity
import br.com.ticket.companion.util.NotificationsExporter

data class MonitoredAppUi(val packageName: String, val displayName: String, val isEnabled: Boolean, val icon: Drawable? = null)
data class InstalledAppUi(val packageName: String, val displayName: String, val icon: Drawable?)
data class SettingsUiState(
    val serverUrl: String = "",
    val companyName: String = "",
    val tokenName: String = "",
    val monitoredApps: List<MonitoredAppUi> = emptyList(),
    val installedApps: List<InstalledAppUi> = emptyList(),
    val showAddAppDialog: Boolean = false,
    val isLoadingApps: Boolean = false,
    val notifySyncSuccess: Boolean = true,
    val notifySyncError: Boolean = true,
    val appVersion: String = "",
    val logs: List<DeliveryLogEntity> = emptyList(),
    val showLogs: Boolean = false,
    val exportMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val monitoredAppDao: MonitoredAppDao,
    private val notificationDao: NotificationDao,
    private val connections: ConnectionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val active = connections.active
        _uiState.value = _uiState.value.copy(
            serverUrl = active?.serverUrl.orEmpty(), companyName = active?.companyName.orEmpty(), tokenName = active?.tokenName.orEmpty(),
            monitoredApps = monitoredAppDao.all().map { MonitoredAppUi(it.packageId, it.label, it.active, icon(it.packageId)) },
            notifySyncSuccess = secureStorage.notifySyncSuccess, notifySyncError = secureStorage.notifySyncError,
            appVersion = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.5.2"
        )
    }

    fun disconnect() { connections.disconnect(); _uiState.value = _uiState.value.copy(serverUrl = "") }
    fun clearAllData() = viewModelScope.launch { notificationDao.deleteAll(); notificationDao.clearLogs() }
    fun showLogs() = viewModelScope.launch { _uiState.value = _uiState.value.copy(logs = notificationDao.getRecentLogs(), showLogs = true) }
    fun hideLogs() { _uiState.value = _uiState.value.copy(showLogs = false) }
    fun export(uri: Uri) = viewModelScope.launch {
        val message = try {
            val json = NotificationsExporter.toJson(notificationDao.getAll())
            withContext(Dispatchers.IO) { checkNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(json) } }
            "Diagnóstico exportado sem texto bancário ou pagador"
        } catch (_: Exception) { "Não foi possível salvar o diagnóstico" }
        _uiState.value = _uiState.value.copy(exportMessage = message)
    }
    fun setNotifySyncSuccess(value: Boolean) { secureStorage.notifySyncSuccess = value; _uiState.value = _uiState.value.copy(notifySyncSuccess = value) }
    fun setNotifySyncError(value: Boolean) { secureStorage.notifySyncError = value; _uiState.value = _uiState.value.copy(notifySyncError = value) }
    fun toggleApp(packageName: String, enabled: Boolean) = viewModelScope.launch { monitoredAppDao.setActive(packageName, enabled); load() }
    fun removeApp(packageName: String) = viewModelScope.launch { monitoredAppDao.forget(packageName); load() }
    fun addApp(app: InstalledAppUi) = viewModelScope.launch {
        monitoredAppDao.save(MonitoredAppEntity(app.packageName, app.displayName)); hideAddAppDialog(); load()
    }
    fun hideAddAppDialog() { _uiState.value = _uiState.value.copy(showAddAppDialog = false, installedApps = emptyList()) }
    fun showAddAppDialog() {
        _uiState.value = _uiState.value.copy(showAddAppDialog = true, isLoadingApps = true)
        viewModelScope.launch {
            val monitored = monitoredAppDao.all().map { it.packageId }.toSet()
            val apps = withContext(Dispatchers.IO) {
                context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
                    .mapNotNull { it.activityInfo.packageName }
                    .filter { it !in monitored && it != context.packageName }
                    .distinct().mapNotNull { pkg ->
                        runCatching {
                            val info = context.packageManager.getApplicationInfo(pkg, 0)
                            InstalledAppUi(pkg, context.packageManager.getApplicationLabel(info).toString(), icon(pkg))
                        }.getOrNull()
                    }.sortedBy { it.displayName.lowercase() }
            }
            _uiState.value = _uiState.value.copy(installedApps = apps, isLoadingApps = false)
        }
    }

    private fun icon(packageName: String) = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
}
