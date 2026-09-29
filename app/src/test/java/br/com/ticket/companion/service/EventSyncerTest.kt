package br.com.ticket.companion.service

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ticket.companion.data.local.AppDatabase
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.connection.ConnectionState
import br.com.ticket.companion.domain.parser.PixClassifier
import br.com.ticket.companion.domain.parser.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class EventSyncerTest {
    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `posts tenant-free event payload and stores reconciliation result`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val event = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", "Banco", "Pix recebido", "Você recebeu um Pix de R$ 12,34", 1_700_000_000_000)!!
            database.notificationDao().insert(event)
            server.enqueue(MockResponse().setBody("""{"results":[{"eventId":"${event.id}","status":"MATCHED","pixChargeId":42,"ticketId":99,"amountCents":1234}]}"""))

            assertEquals(SyncRun.SUCCESS, createSyncer(manager).sync())

            val request = server.takeRequest()
            assertEquals("/backend/companion/pix-events", request.path)
            assertEquals(event.id, request.getHeader("Idempotency-Key"))
            val body = request.body.readUtf8()
            assertFalse(body.contains("companyId"))
            assertFalse(body.contains("tenantId"))
            assertEquals(SyncStatus.DELIVERED, database.notificationDao().getById(event.id)!!.syncStatus)
            assertEquals("MATCHED", database.notificationDao().getById(event.id)!!.reconciliationStatus)
            assertEquals("DELIVERED: MATCHED", database.notificationDao().getRecentLogs().single().message)
        }
    }

    @Test
    fun `received transfer is posted as PIX_RECEIVED because that is the only credit the CRM reconciles`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val event = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", "Nubank", "Transferência recebida na conta PJ", "Você recebeu R$ 12,00 de MARIA SOUZA LIMA na conta Nu Empresas.", 1_700_000_000_000)!!
            database.notificationDao().insert(event)
            server.enqueue(MockResponse().setBody("""{"results":[{"eventId":"${event.id}","status":"UNMATCHED","amountCents":1200}]}"""))

            assertEquals(SyncRun.SUCCESS, createSyncer(manager).sync())

            val request = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS)) { "nenhum pedido chegou ao servidor: o evento não foi enviado" }
            val body = request.body.readUtf8()
            assertTrue(body, body.contains("\"transactionType\":\"PIX_RECEIVED\""))
            assertTrue(body, body.contains("\"direction\":\"INCOMING\""))
            assertTrue(body, body.contains("\"confidence\":\"MEDIUM\""))
            assertFalse(body, body.contains("TRANSFER_RECEIVED"))
            // Localmente o registro continua sendo o que o banco disse: uma transferência recebida.
            assertEquals(TransactionType.TRANSFER_RECEIVED, database.notificationDao().getById(event.id)!!.transactionType)
        }
    }

    @Test
    fun `retired connection event is never sent`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val oldId = manager.active!!.id
            manager.disconnect()
            bind(manager, server, "other")
            val requestCount = server.requestCount
            val event = CapturedEventFactory(PixClassifier()).create(oldId, "bank.app", "Banco", null, "Pix recebido: R$ 10,00", 1_000)!!
            database.notificationDao().insert(event)

            assertEquals(SyncRun.SUCCESS, createSyncer(manager).sync())
            assertEquals(requestCount, server.requestCount)
            assertEquals(SyncStatus.PENDING_SYNC, database.notificationDao().getById(event.id)!!.syncStatus)
        }
    }

    @Test
    fun `auth failure is terminal until reconnect and same event id retries after server failure`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val event = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", "Banco", null, "Pix recebido: R$ 10,00", 1_000)!!
            database.notificationDao().insert(event)
            val syncer = createSyncer(manager)
            server.enqueue(MockResponse().setResponseCode(500))
            assertEquals(SyncRun.RETRY, syncer.sync())
            assertEquals(SyncStatus.PENDING_SYNC, database.notificationDao().getById(event.id)!!.syncStatus)
            server.enqueue(MockResponse().setResponseCode(401))
            assertEquals(SyncRun.FAILURE, syncer.sync(now = Long.MAX_VALUE))
            assertEquals(ConnectionState.AUTH_FAILED, manager.state)
            assertEquals(SyncStatus.SYNC_FAILED, database.notificationDao().getById(event.id)!!.syncStatus)
            assertEquals(event.id, server.takeRequest().getHeader("Idempotency-Key"))
            assertEquals(event.id, server.takeRequest().getHeader("Idempotency-Key"))
        }
    }

    @Test
    fun `individual 400 or 409 error is terminal without failing successful siblings`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val first = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", "Banco", null, "Pix recebido: R$ 10,00", 1_000)!!
            val second = first.copy(id = "second", fingerprint = "second", notificationTimestamp = 61_000)
            database.notificationDao().insert(first)
            database.notificationDao().insert(second)
            server.enqueue(MockResponse().setBody("""{"results":[{"eventId":"${first.id}","status":"ERROR","statusCode":409,"error":"ERR_COMPANION_IDEMPOTENCY_CONFLICT"},{"eventId":"second","status":"UNMATCHED"}]}"""))

            assertEquals(SyncRun.SUCCESS, createSyncer(manager).sync())

            assertEquals(SyncStatus.SYNC_FAILED, database.notificationDao().getById(first.id)!!.syncStatus)
            assertEquals("HTTP_409", database.notificationDao().getById(first.id)!!.lastErrorCode)
            assertEquals(SyncStatus.DELIVERED, database.notificationDao().getById(second.id)!!.syncStatus)
        }
    }

    private suspend fun connectedManager(server: MockWebServer): ConnectionManager {
        val prefs = context.getSharedPreferences("sync-${System.nanoTime()}", Context.MODE_PRIVATE)
        return ConnectionManager(prefs, TicketApiFactory(true)).also { bind(it, server, "company") }
    }

    private fun createSyncer(manager: ConnectionManager) = EventSyncer(
        database.notificationDao(), manager, TicketApiFactory(true),
        br.com.ticket.companion.util.SyncResultNotifier(context, br.com.ticket.companion.util.SecureStorage(context.getSharedPreferences("sync-preferences", Context.MODE_PRIVATE)))
    )

    @Test
    fun `immediate sender and worker share the same durable event without duplicate local attempts`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val event = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", null, null, "Pix recebido: R$ 10,00", 1_000)!!
            database.notificationDao().insert(event)
            server.enqueue(MockResponse().setBody("""{"results":[{"eventId":"${event.id}","status":"UNMATCHED"}]}"""))
            server.enqueue(MockResponse().setResponseCode(500))
            val syncer = createSyncer(manager)
            val outcomes = listOf(async { syncer.sync(event.id) }, async { syncer.sync() }).awaitAll()
            assertEquals(listOf(SyncRun.SUCCESS, SyncRun.SUCCESS), outcomes)
            assertEquals(SyncStatus.DELIVERED, database.notificationDao().getById(event.id)!!.syncStatus)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun `future retries keep worker alive without an early post`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val event = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", null, null, "Pix recebido: R$ 10,00", 1_000)!!.copy(nextAttemptAt = 50_000)
            database.notificationDao().insert(event)
            assertEquals(SyncRun.RETRY, createSyncer(manager).sync(now = 1_000))
            assertEquals(1, server.requestCount)
        }
    }

    @Test
    fun `unknown result is not delivered and retry limit is per event`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val first = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", null, null, "Pix recebido: R$ 10,00", 1_000)!!.copy(attempts = 19)
            val second = first.copy(id = "second", fingerprint = "second", attempts = 0)
            database.notificationDao().insert(first)
            database.notificationDao().insert(second)
            server.enqueue(MockResponse().setBody("""{"results":[{"eventId":"${first.id}","status":"NEW_UNKNOWN"},{"eventId":"second","status":"NEW_UNKNOWN"}]}"""))
            assertEquals(SyncRun.RETRY, createSyncer(manager).sync(now = 1_000))
            assertEquals(SyncStatus.SYNC_FAILED, database.notificationDao().getById(first.id)!!.syncStatus)
            assertEquals(SyncStatus.PENDING_SYNC, database.notificationDao().getById(second.id)!!.syncStatus)
        }
    }

    @Test
    fun `batch limit leaves remaining events scheduled`() = runTest {
        MockWebServer().use { server ->
            val manager = connectedManager(server)
            val first = CapturedEventFactory(PixClassifier()).create(manager.active!!.id, "bank.app", null, null, "Pix recebido: R$ 10,00", 1_000)!!
            (0..20).forEach { database.notificationDao().insert(first.copy(id = "$it", fingerprint = "$it", capturedAt = it.toLong())) }
            server.enqueue(MockResponse().setBody("""{"results":[${(0..19).joinToString(",") { """{"eventId":"$it","status":"UNMATCHED"}""" }}]}"""))
            assertEquals(SyncRun.RETRY, createSyncer(manager).sync())
            assertEquals(1, database.notificationDao().countPending(manager.active!!.id))
        }
    }

    private suspend fun bind(manager: ConnectionManager, server: MockWebServer, slug: String) {
        server.enqueue(MockResponse().setBody("""{"valid":true,"deviceId":"${manager.deviceId}","tokenName":"Caixa","company":{"name":"Empresa","slug":"$slug"}}"""))
        manager.bind(manager.verify(server.url("/").toString(), "token"))
        server.takeRequest()
    }
}
