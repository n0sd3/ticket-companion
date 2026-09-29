package br.com.ticket.companion.domain.connection

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ticket.companion.data.remote.TicketApiFactory
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ConnectionManagerTest {
    @Test fun `validation does not bind and changing company retires the old connection`() = runBlocking {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("connection-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val manager = ConnectionManager(prefs, TicketApiFactory(allowHttp = true))
        MockWebServer().use { server ->
            fun enqueueCompany(slug: String) = server.enqueue(MockResponse().setBody("""{"valid":true,"deviceId":"${manager.deviceId}","tokenName":"Caixa","company":{"name":"$slug","slug":"$slug"}}"""))
            enqueueCompany("loja")
            val candidate = manager.verify(server.url("/").toString(), "token-a")
            assertNull(manager.active)
            val first = manager.bind(candidate)
            assertEquals("loja", first.companySlug)
            assertEquals(first.id, manager.active!!.id)
            manager.disconnect()
            assertNull(manager.active)
            assertEquals(first.id, manager.lastConnection!!.id)
            enqueueCompany("outra")
            val second = manager.bind(manager.verify(server.url("/").toString(), "token-b"))
            assertNotEquals(first.id, second.id)
            assertEquals(listOf(first.id), manager.retiredConnections.map { it.id })
            assertEquals(second.id, ConnectionManager(prefs, TicketApiFactory(true)).active!!.id)
        }
    }

    @Test fun `failed verification cannot replace an existing binding`() = runBlocking {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("rejected-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val manager = ConnectionManager(prefs, TicketApiFactory(true))
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403))
            try {
                manager.verify(server.url("/").toString(), "foreign-token")
                fail("Foreign token must be rejected")
            } catch (_: IllegalStateException) { }
            assertNull(manager.active)
        }
    }

    @Test fun `pending replay is offered only for exact canonical host and company slug`() = runBlocking {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("replay-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val manager = ConnectionManager(prefs, TicketApiFactory(true))
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"valid":true,"deviceId":"${manager.deviceId}","tokenName":"Caixa","company":{"name":"Loja","slug":"loja"}}"""))
            val first = manager.bind(manager.verify(server.url("/").toString(), "token-a"))
            manager.markAuthFailed()
            assertEquals(listOf(first.id), manager.matchingRetired(first.serverUrl, "loja").map { it.id })
            manager.disconnect()

            assertEquals(listOf(first.id), manager.matchingRetired(first.serverUrl, "loja").map { it.id })
            assertTrue(manager.matchingRetired("https://outro.example", "loja").isEmpty())
            assertTrue(manager.matchingRetired(first.serverUrl, "outra").isEmpty())
        }
    }
}
