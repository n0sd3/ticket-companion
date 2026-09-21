package br.com.ticket.companion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.ticket.companion.ui.screens.home.HomeScreen
import br.com.ticket.companion.ui.screens.settings.SettingsScreen
import br.com.ticket.companion.ui.screens.setup.SetupScreen
import br.com.ticket.companion.ui.screens.setup.SetupViewModel
import br.com.ticket.companion.ui.web.WebActivity

private enum class Route(val path: String) { SETUP("setup"), HOME("home"), SETTINGS("settings") }

/** Vai para [target] e remove da pilha tudo até [from] (inclusive). */
private fun NavController.replace(from: Route, target: Route) =
    navigate(target.path) { popUpTo(from.path) { inclusive = true } }

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val setup: SetupViewModel = hiltViewModel()
    val context = LocalContext.current
    val start = remember { if (setup.isConfigured.value) Route.HOME else Route.SETUP }

    NavHost(nav, startDestination = start.path) {
        composable(Route.SETUP.path) {
            SetupScreen(onSetupComplete = { nav.replace(from = Route.SETUP, target = Route.HOME) })
        }
        composable(Route.HOME.path) {
            HomeScreen(
                onOpenSettings = { nav.navigate(Route.SETTINGS.path) },
                onOpenWeb = { setup.webOrigin()?.let { context.startActivity(WebActivity.intent(context, it)) } }
            )
        }
        composable(Route.SETTINGS.path) {
            SettingsScreen(
                onNavigateBack = { nav.popBackStack() },
                onDisconnected = { nav.replace(from = Route.HOME, target = Route.SETUP) }
            )
        }
    }
}
