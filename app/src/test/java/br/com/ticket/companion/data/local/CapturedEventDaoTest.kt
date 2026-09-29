package br.com.ticket.companion.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ticket.companion.data.local.entities.CapturedEventEntity
import br.com.ticket.companion.data.local.entities.SyncStatus
import br.com.ticket.companion.domain.parser.Confidence
import br.com.ticket.companion.domain.parser.Direction
import br.com.ticket.companion.domain.parser.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CapturedEventDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `duplicate fingerprint is ignored and pending query is scoped to active connection`() = runTest {
        val dao = database.notificationDao()
        val active = event(id = "active", connectionId = "connection-a", fingerprint = "same")
        val retired = event(id = "retired", connectionId = "connection-b", fingerprint = "other")

        assertEquals(1L, dao.insert(active))
        assertEquals(-1L, dao.insert(active.copy(id = "duplicate")))
        assertEquals(2L, dao.insert(retired))

        assertEquals(listOf("active"), dao.getPendingSync("connection-a", now = 1_000, limit = 20).map { it.id })
    }

    @Test
    fun `retry and terminal reconciliation fields are persisted`() = runTest {
        val dao = database.notificationDao()
        dao.insert(event(id = "event", connectionId = "connection-a", fingerprint = "fingerprint"))

        dao.markRetry("event", attempts = 3, nextAttemptAt = 5_000, errorCode = "HTTP_500", error = "Servidor indisponível")
        assertEquals(emptyList<CapturedEventEntity>(), dao.getPendingSync("connection-a", now = 4_999, limit = 20))
        assertEquals(listOf("event"), dao.getPendingSync("connection-a", now = 5_000, limit = 20).map { it.id })

        dao.markDelivered("event", "MATCHED", 42, 99, null, deliveredAt = 6_000)
        val delivered = dao.getById("event")!!
        assertEquals(SyncStatus.DELIVERED, delivered.syncStatus)
        assertEquals("MATCHED", delivered.reconciliationStatus)
        assertEquals(42L, delivered.pixChargeId)
        assertEquals(99L, delivered.ticketId)
        assertEquals(6_000L, delivered.deliveredAt)
    }

    private fun event(id: String, connectionId: String, fingerprint: String) = CapturedEventEntity(
        id = id,
        connectionId = connectionId,
        fingerprint = fingerprint,
        sourceApp = "bank.app",
        sourceAppName = "Banco",
        originalTitle = "Pix recebido",
        originalText = "Você recebeu um Pix de R$ 10,00",
        notificationTimestamp = 900,
        capturedAt = 901,
        transactionType = TransactionType.PIX_RECEIVED,
        direction = Direction.INCOMING,
        confidence = Confidence.MEDIUM,
        amountCents = 1_000,
        parserId = "generic.pt-BR.v1",
        syncStatus = SyncStatus.PENDING_SYNC
    )
}
