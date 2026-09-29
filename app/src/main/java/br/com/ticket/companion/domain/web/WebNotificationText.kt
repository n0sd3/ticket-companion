package br.com.ticket.companion.domain.web

/** Texto que vem da página web e vira notificação nativa: limita tamanho e trata vazio. */
object WebNotificationText {
    const val MAX_TITLE = 120
    const val MAX_BODY = 1000

    fun clean(value: String?, fallback: String, max: Int): String =
        value?.trim()?.takeIf { it.isNotEmpty() }?.take(max) ?: fallback

    /** Mesma tag = mesma notificação (atualiza em vez de empilhar). */
    fun idFor(tag: String?, fallbackId: Long): Int =
        tag?.trim()?.takeIf { it.isNotEmpty() }?.hashCode() ?: fallbackId.toInt()
}
