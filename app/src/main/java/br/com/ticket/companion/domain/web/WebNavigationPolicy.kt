package br.com.ticket.companion.domain.web

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

enum class NavigationDecision { LOAD_IN_APP, OPEN_EXTERNAL, BLOCK }

/**
 * Trava o WebView do atendimento em UMA origem (esquema + host + porta).
 *
 * Só a origem travada carrega dentro do app, recebe a ponte nativa de notificações e pode usar
 * câmera, microfone e localização. Qualquer outro http(s) vai para o navegador do aparelho.
 */
class WebNavigationPolicy(originUrl: String) {
    private val origin: HttpUrl = requireNotNull(originUrl.trim().toHttpUrlOrNull()) { "Origem inválida" }

    fun isSameOrigin(url: String?): Boolean {
        val parsed = url?.trim()?.takeIf { it.isNotEmpty() }?.toHttpUrlOrNull() ?: return false
        return parsed.scheme == origin.scheme && parsed.host == origin.host && parsed.port == origin.port
    }

    fun decide(url: String): NavigationDecision {
        val value = url.trim()
        if (value.equals("about:blank", ignoreCase = true)) return NavigationDecision.LOAD_IN_APP
        if (value.toHttpUrlOrNull() != null) {
            return if (isSameOrigin(value)) NavigationDecision.LOAD_IN_APP else NavigationDecision.OPEN_EXTERNAL
        }
        val scheme = value.substringBefore(':', "").lowercase(Locale.ROOT)
        return if (scheme in EXTERNAL_SCHEMES) NavigationDecision.OPEN_EXTERNAL else NavigationDecision.BLOCK
    }

    private companion object {
        val EXTERNAL_SCHEMES = setOf("mailto", "tel", "geo", "whatsapp")
    }
}
