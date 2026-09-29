package br.com.ticket.companion.domain.update

import com.google.gson.JsonParser

data class UpdateInfo(val version: String, val apkUrl: String, val notes: String)

/** Lê a última release do repositório do app e decide se ela é mais nova que a versão instalada. */
object AppUpdate {
    const val LATEST_RELEASE_URL = "https://api.github.com/repos/n0sd3/ticket-companion/releases/latest"
    private const val DOWNLOAD_PREFIX = "https://github.com/n0sd3/ticket-companion/releases/download/"

    fun isNewer(current: String, latest: String): Boolean {
        val a = parts(current) ?: return false
        val b = parts(latest) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    /** Só aceita APK hospedado na release do próprio repositório: o resto da resposta não é confiável. */
    fun parseRelease(json: String): UpdateInfo? = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        if (root.get("draft")?.asBoolean == true) return null
        val version = root.get("tag_name").asString.removePrefix("v")
        val apk = root.getAsJsonArray("assets")
            .map { it.asJsonObject }
            .firstOrNull { it.get("name").asString.endsWith(".apk") && it.get("browser_download_url").asString.startsWith(DOWNLOAD_PREFIX) }
            ?: return null
        UpdateInfo(version, apk.get("browser_download_url").asString, root.get("body")?.takeIf { !it.isJsonNull }?.asString.orEmpty())
    }.getOrNull()

    private fun parts(version: String): List<Int>? =
        version.removePrefix("v").split('.').map { it.toIntOrNull() ?: return null }.takeIf { it.isNotEmpty() }
}
