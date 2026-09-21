package br.com.ticket.companion.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HostPinnedAuthInterceptor(private val origin: HttpUrl, private val token: String, private val deviceId: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.scheme != origin.scheme || request.url.host != origin.host || request.url.port != origin.port) {
            throw IOException("Destino diferente da conexão verificada")
        }
        return chain.proceed(request.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("X-Companion-Device", deviceId)
            .build())
    }
}
