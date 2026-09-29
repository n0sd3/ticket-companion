package br.com.ticket.companion.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class CompanionComponentsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val listener = ComponentName(context, CaptureNotificationListenerService::class.java)

    @Test fun `the listener is only wanted once a company was ever bound`() {
        assertFalse(CompanionComponents.shouldEnableListener(hasActiveConnection = false, hasPastConnection = false))
        assertTrue(CompanionComponents.shouldEnableListener(hasActiveConnection = true, hasPastConnection = false))
        assertTrue(CompanionComponents.shouldEnableListener(hasActiveConnection = false, hasPastConnection = true))
    }

    @Test fun `a rebind is only requested for an enabled listener that is not connected`() {
        assertTrue(CompanionComponents.needsRebind(listenerEnabled = true, connected = false))
        assertFalse(CompanionComponents.needsRebind(listenerEnabled = true, connected = true))
        assertFalse(CompanionComponents.needsRebind(listenerEnabled = false, connected = false))
    }

    @Test fun `the listener ships disabled and can be switched on and off`() {
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, context.packageManager.getComponentEnabledSetting(listener))
        assertFalse(CompanionComponents.isListenerEnabled(context))
        CompanionComponents.setListenerEnabled(context, true)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, context.packageManager.getComponentEnabledSetting(listener))
        assertTrue(CompanionComponents.isListenerEnabled(context))
        CompanionComponents.setListenerEnabled(context, false)
        assertFalse(CompanionComponents.isListenerEnabled(context))
    }
}
