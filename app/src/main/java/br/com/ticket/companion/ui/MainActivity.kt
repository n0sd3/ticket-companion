package br.com.ticket.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.web.WebOrigin
import br.com.ticket.companion.service.CompanionComponents
import br.com.ticket.companion.ui.navigation.AppNavigation
import br.com.ticket.companion.ui.theme.TicketTheme
import br.com.ticket.companion.ui.web.WebActivity
import br.com.ticket.companion.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connections: ConnectionManager
    @Inject lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val companionInUse = alignListenerWithConnection()
        val webOrigin = WebOrigin.normalize(secureStorage.webUrl, connections.allowHttp)
        val landing = LaunchRouter.landing(companionInUse, intent.getBooleanExtra(EXTRA_STAY, false), webOrigin != null)
        if (landing == Landing.WEB && webOrigin != null) {
            startActivity(WebActivity.intent(this, webOrigin))
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            TicketTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AppNavigation() }
            }
        }
    }

    /** O listener de notificações só fica ligado para quem já vinculou uma empresa ao Companion. */
    private fun alignListenerWithConnection(): Boolean {
        val inUse = CompanionComponents.shouldEnableListener(connections.active != null, connections.lastConnection != null)
        if (inUse != CompanionComponents.isListenerEnabled(this)) CompanionComponents.setListenerEnabled(this, inUse)
        return inUse
    }

    companion object {
        /** Ajustes do app abertos a partir do atendimento: não redireciona de volta para o web. */
        const val EXTRA_STAY = "br.com.ticket.companion.STAY"
    }
}
