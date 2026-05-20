package com.example.mybussines.brokers.xtb

object XtbClosedPositionsParser {

    fun parseFromCapturedRequests(requests: List<XtbCapturedRequest>): List<XtbClosedPosition> {
        val request = requests.lastOrNull {
            it.url.contains("PortfolioClosedPositionService/GetClosedPositions", ignoreCase = true)
        } ?: return emptyList()

        val decoded = XtbDebugDecoder.decodePayload(request.responsePreview)
        val strings = decoded.extractedStrings
        val numbers = decoded.extractedNumbers

        if (strings.isEmpty()) return emptyList()

        val symbolRegex = Regex("""[A-Z0-9]{2,10}\.[A-Z]{2,4}""")

        val symbol = strings
            .mapNotNull { symbolRegex.find(it)?.value }
            .firstOrNull()

        val rawTitle = strings.firstOrNull {
            symbol != null && it.contains(symbol)
        }

        val accountCurrency = strings.firstOrNull { it == "PLN" || it == "EUR" || it == "USD" }

        val instrumentCurrency = strings
            .dropWhile { it != accountCurrency }
            .drop(1)
            .firstOrNull { it == "PLN" || it == "EUR" || it == "USD" }

        val logoUrl = strings
            .firstOrNull { it.contains("https://") && it.contains("logos.xtb.com") }
            ?.substringAfter("!")
            ?.substringBefore("@")

        val normalizedNumbers = numbers
            .filter { it > 0.0 }
            .sorted()

        val probableVolume = normalizedNumbers.firstOrNull {
            it == 1.0 || it == 0.1 || it == 0.01 || it == 2.0 || it == 3.0 || it == 5.0 || it == 10.0
        }

        val probablePrices = normalizedNumbers.filter { it in 1.0..100000.0 }
        val probableOpenPrice = probablePrices.firstOrNull()
        val probableClosePrice = probablePrices.drop(1).firstOrNull()

        val probableProfit = numbers.firstOrNull { kotlin.math.abs(it) in 0.01..1_000_000.0 && it != probableOpenPrice && it != probableClosePrice }

        val item = XtbClosedPosition(
            symbol = symbol ?: "Brak symbolu",
            title = cleanTitle(rawTitle ?: "Brak nazwy", symbol),
            accountCurrency = accountCurrency,
            instrumentCurrency = instrumentCurrency,
            logoUrl = logoUrl,
            numericCandidates = numbers,
            profit = probableProfit,
            volume = probableVolume,
            openPrice = probableOpenPrice,
            closePrice = probableClosePrice
        )

        return listOf(item)
    }

    private fun cleanTitle(raw: String, symbol: String?): String {
        var text = raw

        text = text.substringBefore("http")
        text = text.replace("*!", " ")
        text = text.replace("@", " ")

        if (!symbol.isNullOrBlank()) {
            text = text.replace(symbol, "")
        }

        text = text.replace("iShares", "")
        text = text.replace("UCITS", "")
        text = text.replace("ACC", "")
        text = text.replace("ETF", "")
        text = text.replace("ETN", "")

        text = text.replace("EUR", "")
        text = text.replace("USD", "")
        text = text.replace("PLN", "")

        text = text.replace(", 0", "")
        text = text.replace(" 0", "")
        text = text.replace("0,", "")

        text = text.replace("S&p", "S&P")

        text = text.replace(Regex("""\s+,\s+"""), ", ")
        text = text.replace(Regex("""\s+"""), " ")
        text = text.trim(' ', ',', ';')

        if (text.isBlank() && !symbol.isNullOrBlank()) {
            return symbol
        }

        return text
    }
}