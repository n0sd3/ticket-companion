package br.com.ticket.companion.domain.web

import org.junit.Assert.assertEquals
import org.junit.Test

class WebNotificationTextTest {
    @Test fun `empty or missing text falls back and surrounding blanks are trimmed`() {
        assertEquals("Ticket", WebNotificationText.clean(null, "Ticket", 50))
        assertEquals("Ticket", WebNotificationText.clean("   \n", "Ticket", 50))
        assertEquals("Novo ticket", WebNotificationText.clean("  Novo ticket \n", "Ticket", 50))
    }

    @Test fun `text coming from the page is capped so it cannot flood the notification shade`() {
        val capped = WebNotificationText.clean("a".repeat(5000), "x", 100)
        assertEquals(100, capped.length)
        assertEquals(WebNotificationText.MAX_TITLE, 120)
        assertEquals(WebNotificationText.MAX_BODY, 1000)
    }

    @Test fun `the notification id is stable per tag and never reused across blank tags`() {
        assertEquals(WebNotificationText.idFor("ticket-12", 7L), WebNotificationText.idFor("ticket-12", 99L))
        assertEquals(7, WebNotificationText.idFor(null, 7L))
        assertEquals(7, WebNotificationText.idFor("  ", 7L))
    }
}
