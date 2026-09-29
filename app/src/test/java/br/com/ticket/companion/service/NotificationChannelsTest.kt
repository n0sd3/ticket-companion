package br.com.ticket.companion.service

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ticket.companion.ui.web.WebNotifications
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Canais de notificação só existem a partir do Android 8; antes disso criar canal derrubaria o app. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [23])
class NotificationChannelsLegacyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun `creating channels on Android 6 is a harmless no-op`() {
        AppNotificationChannels.ensure(context)
        WebNotifications.ensureChannels(context)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class NotificationChannelsModernTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Test fun `channels are created on modern Android`() {
        assertNull(manager.getNotificationChannel(AppNotificationChannels.SYNC))
        AppNotificationChannels.ensure(context)
        WebNotifications.ensureChannels(context)
        assertNotNull(manager.getNotificationChannel(AppNotificationChannels.SYNC))
        assertNotNull(manager.getNotificationChannel(AppNotificationChannels.RESULTS))
        assertNotNull(manager.getNotificationChannel("web_messages"))
    }
}
