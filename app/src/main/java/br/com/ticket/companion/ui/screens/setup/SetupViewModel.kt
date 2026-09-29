package br.com.ticket.companion.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.ticket.companion.data.local.dao.NotificationDao
import br.com.ticket.companion.domain.connection.ConnectionCandidate
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.connection.ConnectionState
import br.com.ticket.companion.service.CompanionComponents
import br.com.ticket.companion.service.SyncWorker
import br.com.ticket.companion.domain.web.WebOrigin
import br.com.ticket.companion.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val serverUrl: String = "",
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val companyName: String? = null,
    val tokenName: String? = null,
    val canRequeue: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connections: ConnectionManager,
    private val notificationDao: NotificationDao,
    private val secureStorage: SecureStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()
    private val _isConfigured = MutableStateFlow(connections.active != null && connections.state == ConnectionState.CONNECTED)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()
    private var candidate: ConnectionCandidate? = null

    fun updateServerUrl(value: String) { candidate = null; _uiState.value = _uiState.value.copy(serverUrl = value, error = null, companyName = null) }
    fun updateToken(value: String) { candidate = null; _uiState.value = _uiState.value.copy(token = value, error = null, companyName = null) }

    fun verify() = viewModelScope.launch {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.token.isBlank()) {
            _uiState.value = state.copy(error = "Informe o endereço e o token")
            return@launch
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        try {
            val verified = connections.verify(state.serverUrl.trim(), state.token.trim()).also { candidate = it }
            val company = verified.verification.company
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                serverUrl = verified.serverUrl,
                companyName = company.name,
                tokenName = verified.verification.tokenName,
                canRequeue = connections.matchingRetired(verified.serverUrl, company.slug).isNotEmpty()
            )
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "Não foi possível verificar a conexão")
        }
    }

    fun confirm(requeue: Boolean = false) = viewModelScope.launch {
        val verified = candidate ?: return@launch
        candidate = null // um candidato só pode ser confirmado uma vez (duplo toque)
        try {
            val retired = connections.matchingRetired(verified.serverUrl, verified.verification.company.slug).map { it.id }
            val connection = connections.bind(verified)
            if (requeue && retired.isNotEmpty()) notificationDao.reassignPending(retired, connection.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(error = error.message ?: "Não foi possível confirmar a conexão")
            return@launch
        }
        runCatching { CompanionComponents.setListenerEnabled(context, true) }
            .onFailure { Log.w(TAG, "Não foi possível ligar o listener de notificações", it) }
        runCatching { SyncWorker.enqueue(context) }
            .onFailure { Log.w(TAG, "Não foi possível agendar o envio pendente", it) }
        _isConfigured.value = true
    }

    /** Modo só atendimento web (sem token): grava o endereço canônico e devolve a origem a abrir, ou null com erro. */
    fun openWebOnly(): String? {
        val origin = WebOrigin.normalize(_uiState.value.serverUrl, connections.allowHttp)
        if (origin == null) {
            _uiState.value = _uiState.value.copy(error = "Informe o endereço https do Ticket (ex.: https://empresa.exemplo.com)")
            return null
        }
        secureStorage.webUrl = origin
        _uiState.value = _uiState.value.copy(error = null, serverUrl = origin)
        return origin
    }

    /** Origem do atendimento web: a empresa vinculada ao Companion vence o endereço salvo. */
    fun webOrigin(): String? = WebOrigin.launchOrigin(connections.active?.serverUrl, secureStorage.webUrl, connections.allowHttp)

    private companion object { const val TAG = "TicketCompanion" }
}
