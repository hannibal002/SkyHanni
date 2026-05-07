/*
 * Based on lz-string https://github.com/pieroxy/lz-string (MIT License)
 * Copyright (c) 2013 Pieroxy <pieroxy@pieroxy.net>
 */

package at.hannibal2.skyhanni.features.garden.greenhouse

object LZString {

    // URI-safe alphabet used by compressToEncodedURIComponent()
    private const val URI_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$"

    // Fast reverse lookup table
    private val URI_LOOKUP =
        URI_ALPHABET.withIndex().associate { it.value to it.index }

    class BitReader(
        private val input: String,
        private val resetValue: Int = 32,
    ) {

        private var index = 1
        private var value = URI_LOOKUP[input[0]] ?: 0
        private var position = resetValue

        fun readBits(count: Int): Int {

            var bits = 0
            var power = 1

            repeat(count) {

                val bit = if ((value and position) != 0) 1 else 0

                position = position shr 1

                if (position == 0) {

                    position = resetValue

                    if (index < input.length) {
                        value = URI_LOOKUP[input[index++]] ?: 0
                    }
                }

                bits = bits or (bit * power)
                power = power shl 1
            }

            return bits
        }
    }

    // based on https://github.com/pieroxy/lz-string/blob/master/src/_decompress.ts
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun decompressFromEncodedURIComponent(input: String): String {

        if (input.isEmpty()) return ""

        val reader = BitReader(input.replace(" ", "+"))

        val dictionary = mutableListOf("0", "1", "2")

        var enlargeIn = 4
        var dictSize = 4
        var numBits = 3

        /**
         * Reads a character from the stream.
         */

        val firstType = reader.readBits(2)

        val first = when (firstType) {
            0 -> readChar(0, reader)
            1 -> readChar(1, reader)
            2 -> return ""
            else -> error("Invalid stream start")
        }

        dictionary.add(first)

        var w = first

        val result = StringBuilder(first)

        while (true) {
            var code = reader.readBits(numBits)

            when (code) {
                0 -> {
                    val char = readChar(0, reader)

                    dictionary.add(char)

                    code = dictSize
                    dictSize++
                    enlargeIn--
                }

                1 -> {
                    val char = readChar(1, reader)

                    dictionary.add(char)

                    code = dictSize
                    dictSize++
                    enlargeIn--
                }

                2 -> {
                    return result.toString()
                }
            }

            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }

            val entry = when {

                code < dictionary.size -> {
                    dictionary[code]
                }

                code == dictSize -> {
                    w + w[0]
                }

                else -> {
                    error("Bad compressed code: $code")
                }
            }

            result.append(entry)

            // Add new sequence to dictionary
            dictionary.add(w + entry[0])

            dictSize++
            enlargeIn--

            w = entry

            if (enlargeIn == 0) {
                enlargeIn = 1 shl numBits
                numBits++
            }
        }
    }

    fun readChar(type: Int, reader: BitReader): String {
        val bits = when (type) {
            0 -> reader.readBits(8)
            1 -> reader.readBits(16)
            else -> error("Invalid character type: $type")
        }

        return bits.toChar().toString()
    }
}
