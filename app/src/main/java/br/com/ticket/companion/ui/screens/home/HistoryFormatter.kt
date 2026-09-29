package br.com.ticket.companion.ui.screens.home

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatação do histórico. Dinheiro sai de centavos inteiros (nunca de `Double`). */
object HistoryFormatter {
    private val SHORT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    private val FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private val BRAZIL: Locale = Locale.forLanguageTag("pt-BR")

    fun money(cents: Long, locale: Locale = BRAZIL): String =
        NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(cents, 2))

    fun short(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String = SHORT.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun full(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String = FULL.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    fun startOfDay(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
}
