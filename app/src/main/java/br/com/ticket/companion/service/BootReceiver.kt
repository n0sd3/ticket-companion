package br.com.ticket.companion.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Depois de reiniciar o aparelho, retoma a fila de envio pendente. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESTART_ACTIONS) {
            SyncWorker.enqueue(context)
            CompanionComponents.ensureBound(context)
        }
    }

    private companion object {
        val RESTART_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON")
    }
}
