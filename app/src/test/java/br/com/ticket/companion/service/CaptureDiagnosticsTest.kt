package br.com.ticket.companion.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CaptureDiagnosticsTest {
    @Before fun reset() = CaptureDiagnostics.reset()

    @Test fun `starts empty and disconnected`() {
        val s = CaptureDiagnostics.snapshot()
        assertFalse(s.listenerConnected)
        assertNull(s.lastPackage)
        assertEquals(0, s.seen)
    }

    @Test fun `remembers the last notification and why it was dropped`() {
        CaptureDiagnostics.connected(true)
        CaptureDiagnostics.seen("com.nu.production", 1000L)
        CaptureDiagnostics.outcome("sem texto")
        val s = CaptureDiagnostics.snapshot()
        assertTrue(s.listenerConnected)
        assertEquals("com.nu.production", s.lastPackage)
        assertEquals(1000L, s.lastAt)
        assertEquals("sem texto", s.lastOutcome)
        assertEquals(1, s.seen)
    }
}
