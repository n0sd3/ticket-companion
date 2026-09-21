package br.com.ticket.companion.util

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SyncResultNotifierTest {
    @Test fun `notifications respect preferences and contain no banking payload`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val prefs = SecureStorage(app.getSharedPreferences("notifier", Context.MODE_PRIVATE))
        val notifier = SyncResultNotifier(app, prefs)
        val notifications = shadowOf(app.getSystemService(NotificationManager::class.java))
        prefs.notifySyncSuccess = false
        notifier.notify("event", true)
        assertEquals(0, notifications.size())
        prefs.notifySyncSuccess = true
        notifier.notify("event", true)
        assertEquals(1, notifications.size())
        assertEquals("Evento entregue ao Ticket", notifications.allNotifications.single().extras.getCharSequence("android.text"))
    }
}
