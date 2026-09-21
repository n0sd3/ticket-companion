package br.com.ticket.companion.service

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.connection.ConnectionState
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
class HeartbeatTest {
    @Test fun `heartbeat reports listener state and auth rejection stops subsequent sends`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ConnectionManager(context.getSharedPreferences("heartbeat", Context.MODE_PRIVATE), TicketApiFactory(true))
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"valid":true,"deviceId":"${manager.deviceId}","tokenName":"Caixa","company":{"name":"Loja","slug":"loja"}}"""))
            manager.bind(manager.verify(server.url("/").toString(), "token"))
            server.takeRequest()
            val heartbeat = CompanionHeartbeat(context, manager, TicketApiFactory(true))
            server.enqueue(MockResponse().setResponseCode(204))
            assertEquals(SyncRun.SUCCESS, heartbeat.send())
            val request = server.takeRequest()
            assertEquals("/backend/companion/heartbeat", request.path)
            assertEquals("Bearer token", request.getHeader("Authorization"))
            assertTrue(request.body.readUtf8().contains("\"listenerEnabled\":false"))
            server.enqueue(MockResponse().setResponseCode(403))
            assertEquals(SyncRun.FAILURE, heartbeat.send())
            assertEquals(ConnectionState.AUTH_FAILED, manager.state)
            assertEquals(SyncRun.FAILURE, heartbeat.send())
            assertEquals(3, server.requestCount)
        }
    }
}
