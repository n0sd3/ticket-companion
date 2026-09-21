package br.com.ticket.companion.service

import android.os.Build

object SyncScheduling {
    /**
     * Trabalho "expedited" só é pedido no Android 12+. Antes disso o WorkManager o executa como serviço em
     * primeiro plano e exige `getForegroundInfo()`; sem isso o envio falharia com exceção. O envio imediato
     * do listener já cobre o caso, e o worker comum recupera o resto.
     */
    fun canExpedite(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S
}
