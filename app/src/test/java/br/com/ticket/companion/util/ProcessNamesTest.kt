package br.com.ticket.companion.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessNamesTest {
    private fun cmdline(name: String, vararg trailing: Byte) = name.toByteArray() + trailing

    @Test fun `only the main process starts the Companion and the web process does not`() {
        assertTrue(ProcessNames.isMain("br.com.ticket.app", "br.com.ticket.app"))
        assertFalse(ProcessNames.isMain("br.com.ticket.app:web", "br.com.ticket.app"))
        assertTrue("sem nome do processo, não deixa o Companion sem inicializar", ProcessNames.isMain(null, "br.com.ticket.app"))
    }

    @Test fun `the process name is read from cmdline up to the first NUL on old Android`() {
        assertEquals("br.com.ticket.app:web", ProcessNames.fromCmdline(cmdline("br.com.ticket.app:web", 0, 0)))
        assertEquals("br.com.ticket.app", ProcessNames.fromCmdline(cmdline("br.com.ticket.app")))
        assertNull(ProcessNames.fromCmdline(ByteArray(0)))
        assertNull(ProcessNames.fromCmdline(byteArrayOf(0) + "abc".toByteArray()))
    }
}
