package br.com.ticket.companion.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF2F5BEA), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE4FF), onPrimaryContainer = Color(0xFF0A1F5C),
    secondary = Color(0xFF00796B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEEE8), onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF8E5A00), onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7F8FB), onBackground = Color(0xFF191C22),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF191C22),
    surfaceVariant = Color(0xFFE6E9F1), onSurfaceVariant = Color(0xFF444A59),
    outline = Color(0xFF737A8C)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB5C5FF), onPrimary = Color(0xFF0A2A8F),
    primaryContainer = Color(0xFF1B3FC0), onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Color(0xFF7ED8C9), onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048), onSecondaryContainer = Color(0xFFCDEEE8),
    tertiary = Color(0xFFFFB955), onTertiary = Color(0xFF4A2D00),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF12141A), onBackground = Color(0xFFE2E4EC),
    surface = Color(0xFF191C22), onSurface = Color(0xFFE2E4EC),
    surfaceVariant = Color(0xFF444A59), onSurfaceVariant = Color(0xFFC3C8D8),
    outline = Color(0xFF8D93A6)
)

/** Cores de estado que o Material não tem (entregue com sucesso). */
object StatusColors {
    val delivered = Color(0xFF1B7F4B)
}

@Composable
fun TicketTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
