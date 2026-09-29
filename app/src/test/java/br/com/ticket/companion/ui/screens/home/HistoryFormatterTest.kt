package br.com.ticket.companion.ui.screens.home

import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFormatterTest {
    @Test fun `money is shown in reais from integer cents without floating point drift`() {
        val nbsp = ' '
        assertEquals("R$${nbsp}0,01", HistoryFormatter.money(1, Locale("pt", "BR")))
        assertEquals("R$${nbsp}54,90", HistoryFormatter.money(5490, Locale("pt", "BR")))
        assertEquals("R$${nbsp}1.234,56", HistoryFormatter.money(123456, Locale("pt", "BR")))
        assertEquals("R$${nbsp}0,29", HistoryFormatter.money(29, Locale("pt", "BR")))
    }

    @Test fun `dates come out in the local zone in short and full forms`() {
        val moment = 1_758_474_600_000L // 2025-09-21T17:10:00Z
        assertEquals("21/09 17:10", HistoryFormatter.short(moment, ZoneOffset.UTC))
        assertEquals("21/09/2025 17:10:00", HistoryFormatter.full(moment, ZoneOffset.UTC))
        assertEquals("21/09 14:10", HistoryFormatter.short(moment, ZoneOffset.ofHours(-3)))
    }

    @Test fun `the start of today is midnight in the given zone`() {
        val noon = 1_758_474_600_000L
        assertEquals(1_758_412_800_000L, HistoryFormatter.startOfDay(noon, ZoneOffset.UTC))
        assertEquals(1_758_423_600_000L, HistoryFormatter.startOfDay(noon, ZoneOffset.ofHours(-3)))
    }
}
