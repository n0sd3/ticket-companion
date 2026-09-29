package br.com.ticket.companion.domain.parser

object MoneyParser {
    fun toCents(input: String): Long? {
        val value = input.replace('\u00a0', ' ').replace('\u202f', ' ').trim()
            .replace(Regex("^(?:R\\$|RS)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*reais$", RegexOption.IGNORE_CASE), "").trim()
        if (!Regex("-?(?:[0-9]+|[1-9][0-9]{0,2}(?:\\.[0-9]{3})+)(?:,[0-9]{1,2})?").matches(value)) return null
        val parts = value.replace(".", "").split(',')
        return (parts[0] + parts.getOrElse(1) { "" }.padEnd(2, '0')).toLongOrNull()
    }
}
