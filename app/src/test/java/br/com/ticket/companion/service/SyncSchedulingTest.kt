package br.com.ticket.companion.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulingTest {
    @Test fun `expedited work is only requested where it needs no foreground service`() {
        // Antes do Android 12 o WorkManager exigiria getForegroundInfo() e o trabalho falharia com exceção.
        listOf(23, 26, 29, 30).forEach { assertFalse("API $it", SyncScheduling.canExpedite(it)) }
        listOf(31, 33, 35).forEach { assertTrue("API $it", SyncScheduling.canExpedite(it)) }
    }
}
