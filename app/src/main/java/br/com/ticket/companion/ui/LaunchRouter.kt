package br.com.ticket.companion.ui

enum class Landing { WEB, CAPTURE }

/** Decide para onde o app abre: direto no atendimento web ou nas telas de captura de PIX. */
object LaunchRouter {
    fun landing(companionInUse: Boolean, stayInApp: Boolean, hasWebOrigin: Boolean): Landing =
        if (hasWebOrigin && !companionInUse && !stayInApp) Landing.WEB else Landing.CAPTURE
}
