package br.com.ticket.companion.data.remote

import java.net.URI
import java.util.Locale

object ServerUrl {
    fun normalize(input: String, allowHttp: Boolean = false): String {
        val uri = try { URI(input.trim()) } catch (_: Exception) {
            throw IllegalArgumentException("URL inválida")
        }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        require(scheme == "https" || allowHttp && scheme == "http") { "HTTPS obrigatório" }
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "URL deve conter apenas o endereço do servidor" }
        val host = requireNotNull(uri.host).lowercase(Locale.ROOT)
        require(uri.port == -1 || uri.port in 1..65535) { "Porta inválida" }
        if (!allowHttp) {
            require(host.contains('.') && !host.all { it.isDigit() || it == '.' } && !host.contains(':')) { "Use o subdomínio da empresa" }
            require(host.split('.').all { it.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) }) { "Host inválido" }
        }
        val port = if (uri.port == -1 || scheme == "https" && uri.port == 443 || scheme == "http" && uri.port == 80) "" else ":${uri.port}"
        return "$scheme://$host$port"
    }
}
