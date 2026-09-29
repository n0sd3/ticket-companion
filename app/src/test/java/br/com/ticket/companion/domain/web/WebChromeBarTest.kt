package br.com.ticket.companion.domain.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebChromeBarTest {
    @Test fun `the bar shows on the login screen only`() {
        assertTrue(WebChromeBar.showsOn("https://loja.crm.exemplo.com/login"))
        assertTrue(WebChromeBar.showsOn("https://loja.crm.exemplo.com/login/"))
        assertTrue(WebChromeBar.showsOn("https://loja.crm.exemplo.com/login?next=%2Ftickets"))
        assertFalse(WebChromeBar.showsOn("https://loja.crm.exemplo.com/"))
        assertFalse(WebChromeBar.showsOn("https://loja.crm.exemplo.com/tickets/12"))
        assertFalse(WebChromeBar.showsOn("https://loja.crm.exemplo.com/loginx"))
        assertFalse(WebChromeBar.showsOn("https://loja.crm.exemplo.com/a/login"))
    }

    @Test fun `an unknown page keeps the bar hidden`() {
        assertFalse(WebChromeBar.showsOn(null))
        assertFalse(WebChromeBar.showsOn("about:blank"))
    }
}
