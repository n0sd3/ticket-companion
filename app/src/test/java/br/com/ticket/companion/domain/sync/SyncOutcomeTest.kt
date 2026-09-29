package br.com.ticket.companion.domain.sync

import org.junit.Assert.*
import org.junit.Test

class SyncOutcomeTest {
    @Test fun `successful delivery includes rejected business results without transport retry`() {
        assertEquals(SyncAction.DELIVERED, SyncOutcome.fromHttp(200, 0).action)
    }

    @Test fun `authentication and redirects require reconnection while bad items remain terminal`() {
        listOf(401, 403, 301, 302, 307, 308).forEach {
            assertEquals(SyncAction.RECONNECT, SyncOutcome.fromHttp(it, 0).action)
        }
        listOf(400, 409, 422).forEach {
            assertEquals(SyncAction.FAILED, SyncOutcome.fromHttp(it, 0).action)
        }
    }

    @Test fun `network and server errors retry with bounded exponential backoff and attempt limit`() {
        listOf(null, 429, 500, 502, 503, 504).forEach {
            val result = SyncOutcome.fromHttp(it, 0, now = 1000L)
            assertEquals(SyncAction.RETRY, result.action)
            assertEquals(31000L, result.nextAttemptAt)
            assertEquals(3601000L, SyncOutcome.fromHttp(it, 18, now = 1000L).nextAttemptAt)
            assertEquals(SyncAction.FAILED, SyncOutcome.fromHttp(it, 19).action)
        }
    }

    @Test fun `rate limit respects retry after seconds or HTTP date`() {
        assertEquals(121000L, SyncOutcome.fromHttp(429, 0, "120", 1000L).nextAttemptAt)
        assertEquals(120000L, SyncOutcome.fromHttp(429, 0, "Thu, 1 Jan 1970 00:02:00 GMT", 1000L).nextAttemptAt)
        assertEquals(31000L, SyncOutcome.fromHttp(429, 0, "invalid", 1000L).nextAttemptAt)
    }
}
