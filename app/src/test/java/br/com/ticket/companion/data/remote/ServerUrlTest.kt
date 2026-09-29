package br.com.ticket.companion.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerUrlTest {
    @Test fun `normalizes host and removes path with fixed backend prefix supplied separately`() {
        assertEquals("https://loja.example.com", ServerUrl.normalize(" https://LOJA.example.com/backend/ ", false))
        assertEquals("http://10.0.2.2:8080", ServerUrl.normalize("http://10.0.2.2:8080/", true))
    }

    @Test fun `rejects insecure ambiguous and credential bearing release addresses`() {
        listOf(
            "http://loja.example.com", "https://127.0.0.1", "https://[::1]",
            "https://user:pass@loja.example.com", "https://loja.example.com?foo=bar",
            "https://loja.example.com#fragment", "file:///etc/passwd",
            "content://loja", "javascript:alert(1)", "https://", "loja.example.com",
            "https://localhost", "https://loja.example.com\\@evil.example.com"
        ).forEach { input ->
            assertThrows(input, IllegalArgumentException::class.java) { ServerUrl.normalize(input, false) }
        }
    }
}
