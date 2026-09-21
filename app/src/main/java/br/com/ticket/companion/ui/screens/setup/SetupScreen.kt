package br.com.ticket.companion.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.ticket.companion.ui.web.WebActivity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun SetupScreen(onSetupComplete: () -> Unit, viewModel: SetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val configured by viewModel.isConfigured.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(configured) { if (configured) onSetupComplete() }
    Scaffold(topBar = { TopAppBar(title = { Text("Conectar ao Ticket") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Use o endereço com o subdomínio da sua empresa e o token do Companion.")
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                state.serverUrl, viewModel::updateServerUrl, Modifier.fillMaxWidth(),
                label = { Text("Endereço do Ticket") }, placeholder = { Text("https://empresa.exemplo.com") }, singleLine = true, enabled = !state.isLoading
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                state.token, viewModel::updateToken, Modifier.fillMaxWidth(),
                label = { Text("Token do Companion") }, placeholder = { Text("tcmp_...") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !state.isLoading
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            if (state.companyName == null) {
                Button(viewModel::verify, Modifier.fillMaxWidth(), enabled = !state.isLoading) {
                    if (state.isLoading) CircularProgressIndicator() else Text("Verificar")
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Conectado a: ${state.companyName}", style = MaterialTheme.typography.titleMedium)
                        Text("Token: ${state.tokenName}")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button({ viewModel.confirm(false) }, Modifier.fillMaxWidth()) { Text("Confirmar") }
                if (state.canRequeue) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton({ viewModel.confirm(true) }, Modifier.fillMaxWidth()) { Text("Confirmar e reenviar pendentes desta empresa") }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Só usa o atendimento (sem captura de PIX neste aparelho)?", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                { viewModel.openWebOnly()?.let { context.startActivity(WebActivity.intent(context, it)) } },
                Modifier.fillMaxWidth(), enabled = !state.isLoading
            ) { Text("Abrir só o atendimento") }
        }
    }
}
