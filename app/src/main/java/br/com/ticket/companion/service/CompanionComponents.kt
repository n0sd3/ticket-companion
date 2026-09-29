package br.com.ticket.companion.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService

/**
 * O listener de notificações sai DESLIGADO no manifest: quem só usa o atendimento web não vê o app
 * na lista de "Acesso às notificações". Liga quando uma empresa é vinculada ao Companion.
 */
object CompanionComponents {
    fun shouldEnableListener(hasActiveConnection: Boolean, hasPastConnection: Boolean): Boolean =
        hasActiveConnection || hasPastConnection

    fun needsRebind(listenerEnabled: Boolean, connected: Boolean): Boolean = listenerEnabled && !connected

    /**
     * O Android nem sempre reconecta sozinho um listener aprovado (visto num Xiaomi/HyperOS: aprovado e mesmo
     * assim fora da lista de listeners vivos, sem nenhum `onListenerDisconnected`). Pede o vínculo de novo.
     */
    fun ensureBound(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (!needsRebind(isListenerEnabled(context), CaptureDiagnostics.snapshot().listenerConnected)) return
        NotificationListenerService.requestRebind(listener(context))
    }

    fun isListenerEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(listener(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setListenerEnabled(context: Context, enabled: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            listener(context),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun listener(context: Context) = ComponentName(context, CaptureNotificationListenerService::class.java)
}
