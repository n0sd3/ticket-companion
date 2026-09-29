package br.com.ticket.companion.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class TicketApiFactoryTest {
    @Test fun `uses fixed backend prefix and sends credentials only to the configured origin`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"valid":true,"deviceId":"installation","tokenName":"Caixa","company":{"name":"Loja","slug":"loja"}}"""))
            val api = TicketApiFactory(allowHttp = true).create(server.url("/").toString(), "tcmp_secret", "installation")
            assertTrue(api.me().body()!!.valid)
            val request = server.takeRequest()
            assertEquals("/backend/companion/me", request.path)
            assertEquals("Bearer tcmp_secret", request.getHeader("Authorization"))
            assertEquals("installation", request.getHeader("X-Companion-Device"))
        }
    }

    @Test fun `redirect is returned to caller and never reaches the second server`() = runBlocking {
        MockWebServer().use { source -> MockWebServer().use { destination ->
            destination.start()
            source.enqueue(MockResponse().setResponseCode(302).addHeader("Location", destination.url("/stolen")))
            val api = TicketApiFactory(allowHttp = true).create(source.url("/").toString(), "tcmp_secret", "installation")
            assertEquals(302, api.me().code())
            assertEquals(0, destination.requestCount)
        } }
    }

    @Test fun `interceptor aborts a request with another origin before transmitting anything`() {
        MockWebServer().use { allowed -> MockWebServer().use { forbidden ->
            allowed.start()
            forbidden.start()
            val client = OkHttpClient.Builder()
                .addInterceptor(HostPinnedAuthInterceptor(allowed.url("/"), "tcmp_secret", "installation"))
                .build()
            assertThrows(IOException::class.java) {
                client.newCall(Request.Builder().url(forbidden.url("/")).build()).execute().close()
            }
            assertEquals(0, forbidden.requestCount)
        } }
    }
}
