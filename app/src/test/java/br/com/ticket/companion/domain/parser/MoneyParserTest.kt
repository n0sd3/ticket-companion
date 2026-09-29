package br.com.ticket.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParserTest {
    @Test fun `converts Brazilian money without floating point rounding`() {
        mapOf(
            "R$ 1.234,56" to 123456L,
            "RS 0,01" to 1L,
            "R$\u00a054,90" to 5490L,
            "R$\u202f54,90" to 5490L,
            "1.000" to 100000L,
            "54,9 reais" to 5490L,
            "-10,25" to -1025L,
            "0" to 0L
        ).forEach { (input, expected) -> assertEquals(input, expected, MoneyParser.toCents(input)) }
    }

    @Test fun `rejects ambiguous malformed and overflowing amounts`() {
        listOf("", "abc", "1,234", "1.23", "1,234.56", "1.00.000", "1 000", "--1", "9223372036854775807")
            .forEach { assertNull(it, MoneyParser.toCents(it)) }
    }
}
