package br.com.ticket.companion.domain.parser

class PixClassifier {
    // Bank-specific HIGH confidence requires validated notification samples.
    fun classify(pkg: String, title: String?, text: String): ParsedNotification =
        GenericPtBrParser.parse(listOfNotNull(title, text).joinToString("\n"))
}
