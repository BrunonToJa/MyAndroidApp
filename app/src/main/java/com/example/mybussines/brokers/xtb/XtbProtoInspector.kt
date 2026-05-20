package com.example.mybussines.brokers.xtb

import java.nio.ByteBuffer
import java.nio.ByteOrder

object XtbProtoInspector {

    data class ProtoField(
        val fieldNumber: Int,
        val wireType: Int,
        val rawValue: String,
        val interpretedValue: String
    )

    fun inspect(bytes: ByteArray?): List<ProtoField> {
        if (bytes == null || bytes.isEmpty()) return emptyList()

        val fields = mutableListOf<ProtoField>()
        var index = 0

        while (index < bytes.size) {
            try {
                val keyRead = readVarint(bytes, index)
                val key = keyRead.first
                index = keyRead.second

                val fieldNumber = (key ushr 3).toInt()
                val wireType = (key and 0x07).toInt()

                when (wireType) {
                    0 -> {
                        val valueRead = readVarint(bytes, index)
                        val value = valueRead.first
                        index = valueRead.second

                        fields.add(
                            ProtoField(
                                fieldNumber = fieldNumber,
                                wireType = wireType,
                                rawValue = value.toString(),
                                interpretedValue = "varint=$value"
                            )
                        )
                    }

                    1 -> {
                        if (index + 8 > bytes.size) break

                        val slice = bytes.copyOfRange(index, index + 8)
                        val longLE = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN).long
                        val doubleLE = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN).double
                        index += 8

                        fields.add(
                            ProtoField(
                                fieldNumber = fieldNumber,
                                wireType = wireType,
                                rawValue = slice.joinToString(" ") { "%02X".format(it) },
                                interpretedValue = "fixed64 long=$longLE double=$doubleLE"
                            )
                        )
                    }

                    2 -> {
                        val lenRead = readVarint(bytes, index)
                        val len = lenRead.first.toInt()
                        index = lenRead.second

                        if (index + len > bytes.size) break

                        val slice = bytes.copyOfRange(index, index + len)
                        index += len

                        val utf8 = try {
                            String(slice, Charsets.UTF_8)
                        } catch (_: Throwable) {
                            null
                        }

                        val printable = utf8
                            ?.filter { it.code in 32..126 || it == '\n' || it == '\r' || it == '\t' }
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }

                        fields.add(
                            ProtoField(
                                fieldNumber = fieldNumber,
                                wireType = wireType,
                                rawValue = "len=$len hex=${slice.take(40).joinToString(" ") { "%02X".format(it) }}",
                                interpretedValue = if (printable != null) {
                                    "string=$printable"
                                } else {
                                    "bytes(${slice.size})"
                                }
                            )
                        )
                    }

                    5 -> {
                        if (index + 4 > bytes.size) break

                        val slice = bytes.copyOfRange(index, index + 4)
                        val intLE = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN).int
                        val floatLE = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN).float
                        index += 4

                        fields.add(
                            ProtoField(
                                fieldNumber = fieldNumber,
                                wireType = wireType,
                                rawValue = slice.joinToString(" ") { "%02X".format(it) },
                                interpretedValue = "fixed32 int=$intLE float=$floatLE"
                            )
                        )
                    }

                    else -> {
                        fields.add(
                            ProtoField(
                                fieldNumber = fieldNumber,
                                wireType = wireType,
                                rawValue = "unsupported",
                                interpretedValue = "unsupported wire type"
                            )
                        )
                        break
                    }
                }
            } catch (_: Throwable) {
                break
            }
        }

        return fields
    }

    private fun readVarint(bytes: ByteArray, startIndex: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var index = startIndex

        while (index < bytes.size) {
            val b = bytes[index].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            index++

            if ((b and 0x80) == 0) {
                return result to index
            }

            shift += 7
            if (shift > 63) throw IllegalArgumentException("Varint too long")
        }

        throw IllegalArgumentException("Unexpected end of varint")
    }
}