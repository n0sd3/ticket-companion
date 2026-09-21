package br.com.ticket.companion.data.remote

import br.com.ticket.companion.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TicketApiFactory(val allowHttp: Boolean = BuildConfig.DEBUG) {
    fun create(serverUrl: String, token: String, deviceId: String): TicketApi {
        val origin = ServerUrl.normalize(serverUrl, allowHttp).toHttpUrl()
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HostPinnedAuthInterceptor(origin, token, deviceId))
        if (BuildConfig.DEBUG) client.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        })
        return Retrofit.Builder().baseUrl(origin).client(client.build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(TicketApi::class.java)
    }
}
