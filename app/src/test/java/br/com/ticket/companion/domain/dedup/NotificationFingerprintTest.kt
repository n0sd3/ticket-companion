package br.com.ticket.companion.domain.dedup

import org.junit.Assert.*
import org.junit.Test

class NotificationFingerprintTest {
    @Test fun `repost within minute deduplicates but another minute amount or app does not`() {
        val original = NotificationFingerprint.create("bank", "PIX recebido", "R$ 50,00", 5000L, 120000L)
        assertEquals(original, NotificationFingerprint.create("bank", " pix   recebido ", "R$\u00a050,00", 5000L, 179999L))
        assertNotEquals(original, NotificationFingerprint.create("bank", "PIX recebido", "R$ 50,00", 5000L, 180000L))
        assertNotEquals(original, NotificationFingerprint.create("other.bank", "PIX recebido", "R$ 50,00", 5000L, 120000L))
        assertNotEquals(original, NotificationFingerprint.create("bank", "PIX recebido", "R$ 50,00", 6000L, 120000L))
        assertTrue(original.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun `field separator in source text cannot collide with another tuple`() {
        assertNotEquals(
            NotificationFingerprint.create("bank", "a|b", "c", 1L, 0L),
            NotificationFingerprint.create("bank", "a", "b|c", 1L, 0L)
        )
    }
}
