package com.example.mybussines.brokers.xtb

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mybussines.R

class XtbClosedPositionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xtb_closed_positions)

        val tvData = findViewById<TextView>(R.id.tvClosedPositions)

        val store = XtbSessionStore(this)
        val requests = store.getCapturedRequests()
        val positions = XtbClosedPositionsParser.parseFromCapturedRequests(requests)

        if (positions.isEmpty()) {
            tvData.text = "Nie znaleziono zamkniętych pozycji."
            return
        }

        tvData.text = buildString {
            appendLine("Zamknięte pozycje")
            appendLine()

            positions.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.symbol}")
                appendLine("Nazwa: ${item.title}")
                appendLine("Waluta konta: ${item.accountCurrency ?: "-"}")
                appendLine("Waluta instrumentu: ${item.instrumentCurrency ?: "-"}")
                appendLine("Logo: ${item.logoUrl ?: "-"}")
                appendLine("Profit: ${item.profit ?: "-"}")
                appendLine("Volume: ${item.volume ?: "-"}")
                appendLine("Open price: ${item.openPrice ?: "-"}")
                appendLine("Close price: ${item.closePrice ?: "-"}")
                appendLine("Liczby z payloadu: ${item.numericCandidates.joinToString()}")
                appendLine()
            }
        }
    }
}