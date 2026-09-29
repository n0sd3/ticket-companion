package br.com.ticket.companion.data.remote

import br.com.ticket.companion.data.remote.dto.CompanionMe
import br.com.ticket.companion.data.remote.dto.HeartbeatRequest
import br.com.ticket.companion.data.remote.dto.PixEventsRequest
import br.com.ticket.companion.data.remote.dto.PixEventsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Body

interface TicketApi {
    @GET("backend/companion/me")
    suspend fun me(): Response<CompanionMe>

    @POST("backend/companion/pix-events")
    suspend fun pixEvents(@Header("Idempotency-Key") idempotencyKey: String, @Body body: PixEventsRequest): Response<PixEventsResponse>

    @POST("backend/companion/heartbeat")
    suspend fun heartbeat(@Body body: HeartbeatRequest): Response<Unit>
}
