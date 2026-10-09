package at.hannibal2.skyhanni.features.garden.greenhouse

import com.google.gson.JsonParser
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal object SkyMutationsLayoutCodec {

    fun decode(input: String): SkyShardsLayoutCodec.Layout {
        val code = extractCode(input)
        require(code.isNotEmpty()) { "The clipboard does not contain a SkyMutations layout code." }
        val json = UriLzDecoder.decode(code)
            ?: throw IllegalArgumentException("The SkyMutations layout uses invalid compressed data.")
        val entries = runCatching { JsonParser.parseString(json).asJsonArray }
            .getOrElse { throw IllegalArgumentException("The SkyMutations layout does not contain valid JSON.", it) }
        require(entries.size() <= GRID_CELLS) { "The SkyMutations layout contains too many entries." }

        val occupied = mutableSetOf<Pair<Int, Int>>()
        return SkyShardsLayoutCodec.Layout(
            entries.map { element ->
                val entry = element.asJsonArray
                require(entry.size() >= 4) { "A SkyMutations layout entry is incomplete." }
                val sourceX = entry[0].asInt
                val row = entry[1].asInt
                require(sourceX in 0 until GRID_SIZE && row in 0 until GRID_SIZE) {
                    "A SkyMutations placement is outside the Greenhouse grid."
                }
                val column = GRID_SIZE - 1 - sourceX
                require(occupied.add(row to column)) { "The SkyMutations layout contains duplicate placements." }
                SkyShardsLayoutCodec.Placement(
                    cropId = normalizeId(entry[2].asString),
                    row = row,
                    column = column,
                    target = entry[3].asInt == 0,
                )
            },
        )
    }

    private fun extractCode(input: String): String {
        val trimmed = input.trim()
        layoutQuery.find(trimmed)?.groupValues?.get(1)?.let {
            return URLDecoder.decode(it, StandardCharsets.UTF_8)
        }
        return trimmed.substringBefore('#').substringBefore('&')
    }

    private fun normalizeId(name: String): String {
        val normalized = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return idAliases.getOrDefault(normalized, normalized)
    }

    private val idAliases = mapOf(
        "wheat_seeds" to "wheat",
        "melon_seeds" to "melon",
        "pumpkin_seeds" to "pumpkin",
        "dead_plants" to "dead_plant",
    )

    private val layoutQuery = "[?&]layout=([^&#\\s]+)".toRegex(RegexOption.IGNORE_CASE)
    private const val GRID_SIZE = 10
    private const val GRID_CELLS = GRID_SIZE * GRID_SIZE

    private object UriLzDecoder {
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$"

        fun decode(encoded: String): String? {
            if (encoded.isEmpty()) return null
            val input = encoded.replace(' ', '+')
            if (input.any { it !in ALPHABET }) return null
            return decompress(input.length) { ALPHABET.indexOf(input[it]) }
        }

        @Suppress("ReturnCount")
        private fun decompress(length: Int, nextValue: (Int) -> Int): String? {
            val dictionary = mutableListOf("0", "1", "2")
            var enlargeIn = 4
            var dictionarySize = 4
            var bitCount = 3
            var value = nextValue(0)
            var position = 32
            var index = 1

            fun readBits(count: Int): Int {
                var bits = 0
                var power = 1
                repeat(count) {
                    if (value and position != 0) bits = bits or power
                    position = position shr 1
                    if (position == 0) {
                        position = 32
                        value = if (index < length) nextValue(index++) else 0
                    }
                    power = power shl 1
                }
                return bits
            }

            val first = when (readBits(2)) {
                0 -> readBits(8).toChar().toString()
                1 -> readBits(16).toChar().toString()
                2 -> return ""
                else -> return null
            }
            dictionary.add(first)
            var previous = first
            val result = StringBuilder(first)
            while (index <= length) {
                var code = readBits(bitCount)
                when (code) {
                    0 -> {
                        dictionary.add(readBits(8).toChar().toString())
                        code = dictionarySize++
                        enlargeIn--
                    }
                    1 -> {
                        dictionary.add(readBits(16).toChar().toString())
                        code = dictionarySize++
                        enlargeIn--
                    }
                    2 -> return result.toString()
                }
                if (enlargeIn == 0) {
                    enlargeIn = 1 shl bitCount
                    bitCount++
                }
                val entry = dictionary.getOrNull(code)
                    ?: if (code == dictionarySize) previous + previous.first() else return null
                result.append(entry)
                if (result.length > MAX_OUTPUT_CHARS) return null
                dictionary.add(previous + entry.first())
                dictionarySize++
                enlargeIn--
                previous = entry
                if (enlargeIn == 0) {
                    enlargeIn = 1 shl bitCount
                    bitCount++
                }
            }
            return null
        }

        private const val MAX_OUTPUT_CHARS = 16_384
    }
}
