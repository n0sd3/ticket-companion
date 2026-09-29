package br.com.ticket.companion.domain.web

import java.net.URI

/** A barra de título do atendimento só aparece na tela de login; dentro do sistema a página ocupa tudo. */
object WebChromeBar {
    fun showsOn(url: String?): Boolean {
        val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        if (uri.scheme != "https" && uri.scheme != "http") return false
        return uri.path?.trimEnd('/') == "/login"
    }
}
