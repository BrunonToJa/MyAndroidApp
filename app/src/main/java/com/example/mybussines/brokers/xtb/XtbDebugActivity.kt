package com.example.mybussines.brokers.xtb

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mybussines.R

class XtbDebugActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xtb_debug)

        val tvDebug = findViewById<TextView>(R.id.tvDebug)

        val store = XtbSessionStore(this)
        val requests = store.getCapturedRequests()

        val important = requests.filter {
            val url = it.url.lowercase()
            url.contains("subscribebalancesummary") ||
                    url.contains("subscribepersonsummary") ||
                    url.contains("subscribeportfoliopositiongroups") ||
                    url.contains("subscribereportshistory") ||
                    url.contains("getclosedpositions") ||
                    url.contains("getclosedpositionsnetprofit") ||
                    url.contains("subscribetiles") ||
                    url.contains("subscribeordergroups")
                    url.contains("retirement") ||
                    url.contains("retireaccount") ||
                    url.contains("getretirementaccounts") ||
                    url.contains("mainaccountservice") ||
                    url.contains("closedposition") ||
                    url.contains("history") ||
                    url.contains("portfolio") ||
                    url.contains("balance") ||
                    url.contains("order")
        }

        if (important.isEmpty()) {
            tvDebug.text = "Brak zapisanych kluczowych endpointów."
            return
        }

        val text = buildString {
            appendLine("=== XTB DEBUG ===")
            appendLine("Znaleziono: ${important.size}")
            appendLine()

            important.forEachIndexed { index, req ->
                val bodyDecoded = XtbDebugDecoder.decodePayload(req.body)
                val responseDecoded = XtbDebugDecoder.decodePayload(req.responsePreview)

                appendLine("==================================================")
                appendLine("[$index] ${req.method} ${req.url}")
                appendLine()

                appendLine("REQUEST RAW:")
                appendLine(req.body ?: "null")
                appendLine()

                appendLine("REQUEST EXTRACTED STRINGS:")
                if (bodyDecoded.extractedStrings.isEmpty()) {
                    appendLine("null")
                } else {
                    bodyDecoded.extractedStrings.forEach { appendLine("- $it") }
                }
                appendLine()

                appendLine("REQUEST HEX:")
                appendLine(bodyDecoded.hexPreview ?: "null")
                appendLine()

                appendLine("RESPONSE RAW:")
                appendLine(req.responsePreview ?: "null")
                appendLine()

                appendLine("RESPONSE EXTRACTED STRINGS:")
                if (responseDecoded.extractedStrings.isEmpty()) {
                    appendLine("null")
                } else {
                    responseDecoded.extractedStrings.forEach { appendLine("- $it") }
                }
                appendLine()

                appendLine("RESPONSE HEX:")
                appendLine(responseDecoded.hexPreview ?: "null")
                appendLine()

                appendLine("RESPONSE EXTRACTED NUMBERS:")
                if (responseDecoded.extractedNumbers.isEmpty()) {
                    appendLine("null")
                } else {
                    responseDecoded.extractedNumbers.forEach { appendLine("- $it") }
                }
                appendLine()
            }
        }

        tvDebug.text = text
    }
}