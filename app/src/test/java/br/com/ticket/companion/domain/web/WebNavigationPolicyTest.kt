package br.com.ticket.companion.domain.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNavigationPolicyTest {
    private val policy = WebNavigationPolicy("https://loja.crm.exemplo.com")

    @Test fun `same origin stays inside the app regardless of case default port path and query`() {
        listOf(
            "https://loja.crm.exemplo.com/",
            "https://LOJA.crm.exemplo.com/tickets/1?x=1#a",
            "https://loja.crm.exemplo.com:443/backend/public/a.pdf"
        ).forEach {
            assertEquals(it, NavigationDecision.LOAD_IN_APP, policy.decide(it))
            assertTrue(it, policy.isSameOrigin(it))
        }
    }

    @Test fun `any other origin leaves the app and never gets the native bridge`() {
        listOf(
            "https://outra.crm.exemplo.com/",
            "https://loja.crm.exemplo.com.evil.com/",
            "https://evil.com/loja.crm.exemplo.com",
            "https://loja.crm.exemplo.com@evil.com/",
            "http://loja.crm.exemplo.com/",
            "https://loja.crm.exemplo.com:8443/"
        ).forEach {
            assertEquals(it, NavigationDecision.OPEN_EXTERNAL, policy.decide(it))
            assertFalse(it, policy.isSameOrigin(it))
        }
    }

    @Test fun `only harmless external schemes are handed to other apps and dangerous ones are blocked`() {
        listOf("mailto:a@b.com", "tel:+5511999999999", "geo:0,0?q=x", "whatsapp://send?text=oi")
            .forEach { assertEquals(it, NavigationDecision.OPEN_EXTERNAL, policy.decide(it)) }
        listOf(
            "file:///etc/passwd", "content://media/external/file/1", "javascript:alert(1)",
            "intent://x#Intent;scheme=http;end", "data:text/html,<b>x</b>", "blob:https://loja.crm.exemplo.com/uuid", ""
        ).forEach { assertEquals(it, NavigationDecision.BLOCK, policy.decide(it)) }
        assertEquals(NavigationDecision.LOAD_IN_APP, policy.decide("about:blank"))
    }

    @Test fun `web permissions and geolocation are granted only to the locked origin`() {
        assertTrue(policy.isSameOrigin("https://loja.crm.exemplo.com/"))
        listOf(null, "", "   ", "not a url", "https://evil.com").forEach { assertFalse(it.toString(), policy.isSameOrigin(it)) }
    }

    @Test fun `an unusable origin cannot build a policy`() {
        listOf("", "file:///x", "javascript:1", "not a url").forEach {
            try {
                WebNavigationPolicy(it)
                org.junit.Assert.fail("origem inválida aceita: $it")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
