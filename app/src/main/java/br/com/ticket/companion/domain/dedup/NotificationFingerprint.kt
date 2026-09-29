package br.com.ticket.companion.domain.dedup

import java.security.MessageDigest
import java.util.Locale

object NotificationFingerprint {
    fun create(sourceApp: String, title: String?, text: String, amountCents: Long?, postTime: Long): String {
        fun normalize(value: String) = value.lowercase(Locale.ROOT).replace(Regex("[\\s\\u00a0\\u202f]+"), " ").trim()
        val fields = listOf(sourceApp, normalize(title.orEmpty()), normalize(text), amountCents?.toString().orEmpty(), Math.floorDiv(postTime, 60000L).toString())
        val encoded = fields.joinToString("") { "${it.length}:$it" }
        return MessageDigest.getInstance("SHA-256").digest(encoded.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
