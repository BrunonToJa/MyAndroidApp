package com.example.mybussines.brokers.xtb

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object XtbDebugDecoder {

    data class DecodedResult(
        val raw: String?,
        val decodedBytes: ByteArray?,
        val printableText: String?,
        val extractedStrings: List<String>,
        val hexPreview: String?,
        val extractedNumbers: List<Double>
    )

    fun decodePayload(raw: String?): DecodedResult {
        if (raw.isNullOrBlank()) {
            return DecodedResult(
                raw = raw,
                decodedBytes = null,
                printableText = null,
                extractedStrings = emptyList(),
                hexPreview = null,
                extractedNumbers = emptyList()
            )
        }

        return try {
            val decodedBytes = Base64.decode(raw.trim(), Base64.DEFAULT)

            val printable = buildString {
                decodedBytes.forEach { byte ->
                    val c = byte.toInt().toChar()
                    if (c.code in 32..126 || c == '\n' || c == '\r' || c == '\t') {
                        append(c)
                    } else {
                        append(' ')
                    }
                }
            }.replace(Regex("\\s+"), " ").trim().ifBlank { null }

            val extracted = extractReadableStrings(decodedBytes)
            val numbers = extractDoublesFromBytes(decodedBytes)

            val hex = decodedBytes
                .take(200)
                .joinToString(" ") { "%02X".format(it) }

            DecodedResult(
                raw = raw,
                decodedBytes = decodedBytes,
                printableText = printable,
                extractedStrings = extracted,
                hexPreview = hex,
                extractedNumbers = numbers
            )
        } catch (_: Throwable) {
            DecodedResult(
                raw = raw,
                decodedBytes = null,
                printableText = null,
                extractedStrings = emptyList(),
                hexPreview = null,
                extractedNumbers = emptyList()
            )
        }
    }

    private fun extractReadableStrings(bytes: ByteArray): List<String> {
        val results = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            val text = current.toString().trim()
            if (text.length >= 3) {
                results.add(text)
            }
            current.clear()
        }

        bytes.forEach { byte ->
            val c = byte.toInt().toChar()
            if (c.code in 32..126) {
                current.append(c)
            } else {
                flush()
            }
        }
        flush()

        return results
            .map { it.trim() }
            .filter { it.length >= 3 }
            .distinct()
    }

    private fun extractDoublesFromBytes(bytes: ByteArray): List<Double> {
        if (bytes.size < 8) return emptyList()

        val result = mutableListOf<Double>()

        for (i in 0..bytes.size - 8) {
            try {
                val le = ByteBuffer.wrap(bytes, i, 8).order(ByteOrder.LITTLE_ENDIAN).double
                if (isReasonableNumber(le)) result.add(round6(le))
            } catch (_: Throwable) {
            }

            try {
                val be = ByteBuffer.wrap(bytes, i, 8).order(ByteOrder.BIG_ENDIAN).double
                if (isReasonableNumber(be)) result.add(round6(be))
            } catch (_: Throwable) {
            }
        }

        return result.distinct().take(30)
    }

    private fun isReasonableNumber(value: Double): Boolean {
        if (!value.isFinite()) return false
        if (value == 0.0) return false
        if (abs(value) < 0.0000001) return false
        if (abs(value) > 100_000_000) return false
        return true
    }

    private fun round6(value: Double): Double {
        return kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    }
}