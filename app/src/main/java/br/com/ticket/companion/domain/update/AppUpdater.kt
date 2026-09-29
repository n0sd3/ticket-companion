package br.com.ticket.companion.domain.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Baixa a release mais nova e entrega ao instalador do Android. A assinatura é conferida pelo próprio sistema. */
class AppUpdater(private val context: Context) {
    fun check(currentVersion: String): UpdateInfo? {
        val body = get(AppUpdate.LATEST_RELEASE_URL, "application/vnd.github+json")
        return AppUpdate.parseRelease(body)?.takeIf { AppUpdate.isNewer(currentVersion, it.version) }
    }

    fun download(info: UpdateInfo, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val target = File(dir, "ticket-${info.version}.apk")
        val connection = open(info.apkUrl, "application/octet-stream")
        val total = connection.contentLength.toLong() // APK cabe em Int; contentLengthLong exige API 24
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    done += read
                    if (total > 0) onProgress((done * 100 / total).toInt())
                }
            }
        }
        if (target.length() == 0L) throw java.io.IOException("APK vazio")
        return target
    }

    fun canInstall(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Leva à tela "Instalar apps desconhecidas" do próprio app; a permissão é do usuário. */
    fun permissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun get(url: String, accept: String): String = open(url, accept).inputStream.bufferedReader().use { it.readText() }

    private fun open(url: String, accept: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", accept)
        connection.instanceFollowRedirects = true // o asset da release redireciona para o CDN
        if (connection.responseCode !in 200..299) throw java.io.IOException("HTTP ${connection.responseCode}")
        return connection
    }
}
