package br.com.ticket.companion.domain.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebOriginTest {
    @Test fun `the verified company connection always wins over a saved address`() {
        assertEquals(
            "https://loja.crm.exemplo.com",
            WebOrigin.launchOrigin("https://loja.crm.exemplo.com", "https://outra.crm.exemplo.com", allowHttp = false)
        )
    }

    @Test fun `a saved address is normalized to a canonical origin when there is no connection`() {
        assertEquals(
            "https://loja.crm.exemplo.com",
            WebOrigin.launchOrigin(null, " https://Loja.Crm.Exemplo.com/algum/caminho/ ", allowHttp = false)
        )
    }

    @Test fun `nothing usable means no origin instead of a silent default`() {
        assertNull(WebOrigin.launchOrigin(null, null, allowHttp = false))
        assertNull(WebOrigin.launchOrigin(null, "", allowHttp = false))
        listOf("http://loja.crm.exemplo.com", "https://192.168.0.10", "https://localhost", "file:///x", "javascript:1", "lixo", "https://u:p@loja.crm.exemplo.com")
            .forEach { assertNull(it, WebOrigin.normalize(it, allowHttp = false)) }
    }

    @Test fun `cleartext is accepted only in debug builds`() {
        assertEquals("http://10.0.2.2:3000", WebOrigin.normalize("http://10.0.2.2:3000/", allowHttp = true))
        assertNull(WebOrigin.normalize("http://10.0.2.2:3000/", allowHttp = false))
    }
}
