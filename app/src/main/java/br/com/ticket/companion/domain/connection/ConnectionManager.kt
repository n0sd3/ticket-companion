package br.com.ticket.companion.domain.connection

import android.content.SharedPreferences
import br.com.ticket.companion.data.remote.ServerUrl
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.data.remote.dto.CompanionMe
import com.google.gson.Gson
import java.net.URI
import java.util.UUID

data class Connection(val id: String, val serverUrl: String, val accessToken: String, val companyName: String, val companySlug: String, val tokenName: String)
data class RetiredConnection(val id: String, val serverUrl: String, val companyName: String, val companySlug: String)
enum class ConnectionState { DISCONNECTED, CONNECTED, AUTH_FAILED }
class ConnectionCandidate internal constructor(val serverUrl: String, internal val token: String, val verification: CompanionMe)

class ConnectionManager(private val prefs: SharedPreferences, private val factory: TicketApiFactory) {
    private val gson = Gson()
    private var pending: ConnectionCandidate? = null
    private var generation = 0L
    val allowHttp: Boolean get() = factory.allowHttp
    val deviceId: String get() = synchronized(this) {
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            check(prefs.edit().putString("device_id", it).commit()) { "Não foi possível salvar a identidade do aparelho" }
        }
    }
    val active: Connection? get() = gson.fromJson(prefs.getString("active_connection", null), Connection::class.java)
    val lastConnection: RetiredConnection? get() = gson.fromJson(prefs.getString("last_connection", null), RetiredConnection::class.java)
    val retiredConnections: List<RetiredConnection> get() = gson.fromJson(prefs.getString("retired_connections", "[]"), Array<RetiredConnection>::class.java).toList()
    fun matchingRetired(serverUrl: String, companySlug: String) =
        (retiredConnections + listOfNotNull(active?.takeIf { state == ConnectionState.AUTH_FAILED }?.metadata()))
            .distinctBy { it.id }.filter { it.serverUrl == serverUrl && it.companySlug == companySlug }
    val state: ConnectionState get() = prefs.getString("connection_state", null)?.let(ConnectionState::valueOf)
        ?: if (active == null) ConnectionState.DISCONNECTED else ConnectionState.CONNECTED

    suspend fun verify(serverUrl: String, token: String): ConnectionCandidate {
        val attempt = synchronized(this) { pending = null; ++generation }
        val normalized = ServerUrl.normalize(serverUrl, factory.allowHttp)
        val response = factory.create(normalized, token, deviceId).me()
        check(response.isSuccessful) {
            when (response.code()) {
                401, 403 -> "Token recusado pela empresa deste endereço"
                404 -> "Empresa não encontrada neste endereço"
                in 300..399 -> "O endereço redirecionou para outro servidor; confira a URL"
                else -> "Não foi possível verificar a conexão"
            }
        }
        val body = checkNotNull(response.body()) { "Resposta de verificação ausente" }
        check(body.valid && body.deviceId == deviceId && !body.company?.slug.isNullOrBlank() && !body.company?.name.isNullOrBlank() && !body.tokenName.isNullOrBlank()) { "Resposta de verificação inválida" }
        check(factory.allowHttp || URI(normalized).host.substringBefore('.') == body.company.slug) { "Empresa diferente da URL verificada" }
        return synchronized(this) {
            check(attempt == generation) { "Verificação substituída por outra conexão" }
            ConnectionCandidate(normalized, token, body).also { pending = it }
        }
    }

    @Synchronized
    fun bind(candidate: ConnectionCandidate): Connection {
        check(candidate === pending) { "Verifique a conexão antes de confirmar" }
        val body = candidate.verification
        val connection = Connection(UUID.randomUUID().toString(), candidate.serverUrl, candidate.token, body.company.name, body.company.slug, body.tokenName)
        val retired = retiredConnections + listOfNotNull(active?.metadata())
        check(prefs.edit()
            .putString("active_connection", gson.toJson(connection))
            .putString("last_connection", gson.toJson(connection.metadata()))
            .putString("retired_connections", gson.toJson(retired))
            .putString("connection_state", ConnectionState.CONNECTED.name)
            .commit()) { "Não foi possível salvar a conexão" }
        pending = null
        generation++
        return connection
    }

    @Synchronized
    fun disconnect() {
        val retired = retiredConnections + listOfNotNull(active?.metadata())
        check(prefs.edit().remove("active_connection")
            .putString("connection_state", ConnectionState.DISCONNECTED.name)
            .putString("retired_connections", gson.toJson(retired)).commit()) { "Não foi possível desconectar" }
        pending = null
        generation++
    }

    fun markAuthFailed() {
        check(prefs.edit().putString("connection_state", ConnectionState.AUTH_FAILED.name).commit()) { "Não foi possível salvar o estado da conexão" }
    }

    private fun Connection.metadata() = RetiredConnection(id, serverUrl, companyName, companySlug)
}
