package br.com.ticket.companion.domain.web

import br.com.ticket.companion.data.remote.ServerUrl

/** Decide qual endereço o atendimento web abre, sempre em forma canônica (esquema://host[:porta]). */
object WebOrigin {
    fun normalize(input: String?, allowHttp: Boolean): String? =
        input?.takeIf { it.isNotBlank() }?.let { runCatching { ServerUrl.normalize(it, allowHttp) }.getOrNull() }

    /** A empresa conectada ao Companion (já verificada) vence; sem ela, vale o endereço salvo. */
    fun launchOrigin(connectionUrl: String?, savedUrl: String?, allowHttp: Boolean): String? =
        normalize(connectionUrl, allowHttp) ?: normalize(savedUrl, allowHttp)
}
