package at.hannibal2.skyhanni.features.garden.greenhouse

import java.math.BigInteger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.DataFormatException
import java.util.zip.Inflater

internal object SkyLayoutsLayoutCodec {

    fun decode(input: String): SkyShardsLayoutCodec.Layout {
        require(input.contains(SKYLAYOUTS_HOST, ignoreCase = true)) {
            "The clipboard does not contain a SkyLayouts link."
        }
        val encoded = shortLinkPattern.find(input)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("The clipboard does not contain a SkyLayouts short link.")
        return decodeShortLink(URLDecoder.decode(encoded.replace("\\", ""), StandardCharsets.UTF_8))
    }

    private fun decodeShortLink(code: String): SkyShardsLayoutCodec.Layout {
        require(code.length <= MAX_SHORT_CODE_LENGTH) { "The SkyLayouts short code is too long." }
        val separator = code.indexOf('~')
        val header = if (separator < 0) code else code.substring(0, separator)
        val encodedBoards = if (separator < 0) "" else code.substring(separator + 1)
        require(header.length >= SHORT_HEADER_LENGTH && header[0] == SHORT_FORMAT_VERSION) {
            "The SkyLayouts link uses an unsupported short layout format."
        }
        require(header.drop(1).all { it in SHORT_ALPHABET }) {
            "The SkyLayouts short code contains an invalid header."
        }
        val selectedPlot = header.drop(SHORT_HEADER_LENGTH).firstOrNull { it == '2' || it == '3' }
            ?.digitToInt() ?: 1
        require(encodedBoards.isNotEmpty()) { "The SkyLayouts short link does not contain layout data." }

        val boards = when (encodedBoards.first()) {
            'p' -> decodePackedBoards(encodedBoards.substring(1))
            'z' -> decodeCompressedBoards(encodedBoards)
            else -> throw IllegalArgumentException("The SkyLayouts link uses an unsupported board encoding.")
        }
        val cells = boards[selectedPlot]
            ?: throw IllegalArgumentException("The SkyLayouts short link does not contain Plot $selectedPlot data.")
        return layoutFromCells(cells)
    }

    private fun decodePackedBoards(encoded: String): Map<Int, List<String?>> {
        val boards = mutableMapOf<Int, List<String?>>()
        encoded.split('~').take(MAX_PLOTS).forEachIndexed { index, board ->
            val plot = index + 1
            if (board.isEmpty()) return@forEachIndexed
            if (board == "1" || board == "2") {
                boards[board.toInt()]?.let { boards[plot] = it.toList() }
                return@forEachIndexed
            }
            boards[plot] = decodeVersionThreeBoard(board)
        }
        return boards
    }

    private fun decodeVersionThreeBoard(encoded: String): List<String?> {
        require(encoded.length >= 3 && encoded[0] == '3') {
            "The SkyLayouts short link contains an unsupported packed board."
        }
        val size = shortValue(encoded[1])
        val paletteSize = shortValue(encoded[2])
        require(size == GRID_SIZE && paletteSize >= 0 && encoded.length >= 3 + paletteSize) {
            "The SkyLayouts short link contains an invalid packed board header."
        }
        if (paletteSize == 0) return List(GRID_CELLS) { null }
        val palette = encoded.substring(3, 3 + paletteSize).map { token ->
            shortCropIds.getOrNull(shortValue(token))
                ?: throw IllegalArgumentException("The SkyLayouts short link contains an unknown crop index.")
        }
        var packed = BigInteger.ZERO
        encoded.substring(3 + paletteSize).forEach { token ->
            packed = packed.shiftLeft(6).or(BigInteger.valueOf(shortValue(token).toLong()))
        }
        val base = BigInteger.valueOf((paletteSize + 1).toLong())
        val reversed = mutableListOf<Int>()
        while (packed > BigInteger.ONE) {
            require(reversed.size < GRID_CELLS) { "The SkyLayouts packed board contains too many cells." }
            val division = packed.divideAndRemainder(base)
            reversed.add(division[1].toInt() - 1)
            packed = division[0]
        }
        require(reversed.size == GRID_CELLS) { "The SkyLayouts packed board is incomplete." }
        return reversed.asReversed().map { paletteIndex -> palette.getOrNull(paletteIndex) }
    }

    private fun decodeCompressedBoards(encoded: String): Map<Int, List<String?>> {
        require(encoded.length >= 4) { "The SkyLayouts compressed board header is incomplete." }
        val size = shortValue(encoded[1])
        val plotMask = shortValue(encoded[2])
        val paletteSize = shortValue(encoded[3])
        require(size == GRID_SIZE && plotMask in 1 until (1 shl MAX_PLOTS) && encoded.length >= 4 + paletteSize) {
            "The SkyLayouts short link contains an invalid compressed board header."
        }
        val palette = encoded.substring(4, 4 + paletteSize).map { token ->
            shortCropIds.getOrNull(shortValue(token))
                ?: throw IllegalArgumentException("The SkyLayouts short link contains an unknown crop index.")
        }
        val plots = (1..MAX_PLOTS).filter { plotMask and (1 shl (it - 1)) != 0 }
        val expectedBytes = plots.size * GRID_CELLS
        val cells = inflateRaw(decodeShortBase64(encoded.substring(4 + paletteSize)), expectedBytes)
        return plots.mapIndexed { boardIndex, plot ->
            plot to List(GRID_CELLS) { cellIndex ->
                val paletteIndex = cells[boardIndex * GRID_CELLS + cellIndex].toInt() and 0xff
                if (paletteIndex == 0) null else palette.getOrNull(paletteIndex - 1)
                    ?: throw IllegalArgumentException("The SkyLayouts board references an unknown palette entry.")
            }
        }.toMap()
    }

    private fun decodeShortBase64(encoded: String): ByteArray {
        val output = mutableListOf<Byte>()
        var bits = 0
        var bitCount = 0
        encoded.forEach { token ->
            bits = bits shl 6 or shortValue(token)
            bitCount += 6
            if (bitCount >= 8) {
                bitCount -= 8
                output.add((bits shr bitCount and 0xff).toByte())
                bits = bits and ((1 shl bitCount) - 1)
            }
        }
        return output.toByteArray()
    }

    private fun inflateRaw(compressed: ByteArray, expectedBytes: Int): ByteArray {
        val inflater = Inflater(true)
        return try {
            inflater.setInput(compressed)
            val output = ByteArray(expectedBytes)
            val length = inflater.inflate(output)
            require(inflater.finished() && length == expectedBytes) {
                "The SkyLayouts compressed board is incomplete or too large."
            }
            output
        } catch (exception: DataFormatException) {
            throw IllegalArgumentException("The SkyLayouts link uses invalid compressed board data.", exception)
        } finally {
            inflater.end()
        }
    }

    private fun layoutFromCells(cells: List<String?>): SkyShardsLayoutCodec.Layout =
        SkyShardsLayoutCodec.Layout(
            cells.mapIndexedNotNull { index, cropId ->
                cropId ?: return@mapIndexedNotNull null
                SkyShardsLayoutCodec.Placement(
                    cropId = normalizeId(cropId),
                    row = index / GRID_SIZE,
                    column = index % GRID_SIZE,
                    target = false,
                )
            },
        )

    private fun shortValue(token: Char): Int = SHORT_ALPHABET.indexOf(token).also {
        require(it >= 0) { "The SkyLayouts short code contains an invalid character: $token" }
    }

    private fun normalizeId(id: String): String = id.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    private val shortLinkPattern = "/l/([^?#\\s\\])>]+)".toRegex(RegexOption.IGNORE_CASE)
    private const val SKYLAYOUTS_HOST = "skylayouts.io"
    private const val GRID_SIZE = 10
    private const val GRID_CELLS = GRID_SIZE * GRID_SIZE
    private const val SHORT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private const val SHORT_FORMAT_VERSION = '1'
    private const val SHORT_HEADER_LENGTH = 3
    private const val MAX_SHORT_CODE_LENGTH = 4096
    private const val MAX_PLOTS = 3

    private val shortCropIds = listOf(
        "ALL_IN_ALOE", "ASHWREATH", "BLASTBERRY", "BROWN_MUSHROOM", "CACTUS", "CARROT", "CHEESEBITE",
        "CHLORONITE", "CHOCOBERRY", "CHOCONUT", "CHORUS_FRUIT", "CINDERSHADE", "COALROOT", "COCOA_BEANS",
        "CREAMBLOOM", "DEAD_PLANT", "DEVOURER", "DO_NOT_EAT_SHROOM", "DUSKBLOOM", "DUSTGRAIN", "FERMENTO",
        "FIRE", "FLESHTRAP", "GLASSCORN", "GLOOMGOURD", "GODSEED", "LONELILY", "MAGIC_JELLYBEAN", "MELON",
        "MOONFLOWER", "NETHER_WART", "NOCTILUME", "PHANTOMLEAF", "PLANTBOY_ADVANCE", "POTATO", "PUFFERCLOUD",
        "PUMPKIN", "RED_MUSHROOM", "SCOURROOT", "SHADEVINE", "SHELLFRUIT", "SNOOZLING", "SOGGYBUD",
        "STARTLEVINE", "STOPLIGHT_PETAL", "SUGAR_CANE", "SUNFLOWER", "THORNSHADE", "THUNDERLING", "TIMESTALK",
        "TURTLELLINI", "VEILSHROOM", "WHEAT", "WILD_ROSE", "WITHERBLOOM", "ZOMBUD",
    )
}
