package br.com.ticket.companion

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.ticket.companion.service.AppNotificationChannels
import br.com.ticket.companion.service.CompanionComponents
import br.com.ticket.companion.service.HeartbeatWorker
import br.com.ticket.companion.util.ProcessNames
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TicketCompanionApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // O atendimento web roda no processo ":web": ali não se agenda nada do Companion.
        if (ProcessNames.isMain(ProcessNames.current(), packageName)) startCompanion()
    }

    private fun startCompanion() {
        AppNotificationChannels.ensure(this)
        HeartbeatWorker.schedule(this)
        CompanionComponents.ensureBound(this)
    }
}
