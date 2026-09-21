package br.com.ticket.companion.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * O listener de notificações sai DESLIGADO no manifest: quem só usa o atendimento web não vê o app
 * na lista de "Acesso às notificações". Liga quando uma empresa é vinculada ao Companion.
 */
object CompanionComponents {
    fun shouldEnableListener(hasActiveConnection: Boolean, hasPastConnection: Boolean): Boolean =
        hasActiveConnection || hasPastConnection

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
