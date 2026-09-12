package at.hannibal2.skyhanni.features.garden.greenhouse

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.DataFormatException
import java.util.zip.Inflater

internal object SkyShardsLayoutCodec {

    data class Layout(val placements: List<Placement>) {
        val inputs get() = placements.filterNot { it.target }
        val targets get() = placements.filter { it.target }
    }

    data class Placement(
        val cropId: String,
        val row: Int,
        val column: Int,
        val target: Boolean,
    )

    fun decode(input: String): Layout {
        val code = extractCode(input)
        require(code.isNotEmpty()) { "The clipboard does not contain a SkyShards layout code." }
        val compressed = try {
            Base64.getUrlDecoder().decode(code)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("The SkyShards layout code is not valid Base64.", exception)
        }
        return parse(inflate(compressed))
    }

    private fun extractCode(input: String): String {
        val trimmed = input.trim()
        layoutQuery.find(trimmed)?.groupValues?.get(1)?.let {
            return URLDecoder.decode(it, StandardCharsets.UTF_8)
        }
        sharePath.find(trimmed)?.groupValues?.get(1)?.let { return it }
        return trimmed.substringBefore('#').substringBefore('&')
    }

    private fun inflate(compressed: ByteArray): String = try {
        inflate(compressed, noWrap = true)
    } catch (rawException: IllegalArgumentException) {
        try {
            inflate(compressed, noWrap = false)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("The SkyShards layout uses invalid compressed data.", rawException)
        }
    }

    private fun inflate(compressed: ByteArray, noWrap: Boolean): String {
        val inflater = Inflater(noWrap)
        return try {
            inflater.setInput(compressed)
            val output = ByteArray(MAX_DECOMPRESSED_BYTES)
            val length = inflater.inflate(output)
            require(inflater.finished()) { "The decompressed SkyShards layout is too large or incomplete." }
            output.decodeToString(endIndex = length)
        } catch (exception: DataFormatException) {
            throw IllegalArgumentException("Invalid compressed data.", exception)
        } finally {
            inflater.end()
        }
    }

    private fun parse(serialized: String): Layout {
        val parts = serialized.split('|')
        require(parts.size == 3) { "Invalid SkyShards layout: expected three sections." }
        val inputIds = parseIds(parts[0])
        val targetIds = parseIds(parts[1])
        val grid = parts[2]
        val tokenLength = when (grid.length) {
            GRID_CELLS -> 1
            GRID_CELLS * 2 -> 2
            else -> throw IllegalArgumentException(
                "Invalid SkyShards grid: expected $GRID_CELLS or ${GRID_CELLS * 2} characters.",
            )
        }
        val emptyToken = ".".repeat(tokenLength)
        return Layout(
            buildList {
                repeat(GRID_CELLS) { index ->
                    val token = grid.substring(index * tokenLength, (index + 1) * tokenLength)
                    if (token == emptyToken) return@repeat
                    val target = token.all(Char::isUpperCase)
                    val input = token.all(Char::isLowerCase)
                    if (!target && !input) return@repeat
                    val idIndex = tokenIndex(token)
                    val ids = if (target) targetIds else inputIds
                    val cropId = ids.getOrNull(idIndex) ?: return@repeat
                    add(Placement(cropId, index / GRID_SIZE, index % GRID_SIZE, target))
                }
            },
        )
    }

    private fun parseIds(serialized: String): List<String> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(',').map { encoded ->
            val index = encoded.toIntOrNull(36)
                ?: throw IllegalArgumentException("Invalid SkyShards crop index: $encoded")
            allIds.getOrNull(index)
                ?: throw IllegalArgumentException("Unknown SkyShards crop index: $encoded")
        }
    }

    private fun tokenIndex(token: String): Int {
        val normalized = token.lowercase()
        return when (normalized.length) {
            1 -> ALPHABET.indexOf(normalized[0])
            2 -> ALPHABET.indexOf(normalized[0]) * ALPHABET.length + ALPHABET.indexOf(normalized[1])
            else -> -1
        }
    }

    private val layoutQuery = "[?&]layout=([^&#\\s]+)".toRegex(RegexOption.IGNORE_CASE)
    private val sharePath = "/share/([^/?#\\s]+)".toRegex(RegexOption.IGNORE_CASE)
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"
    private const val GRID_SIZE = 10
    private const val GRID_CELLS = GRID_SIZE * GRID_SIZE
    private const val MAX_DECOMPRESSED_BYTES = 4096

    private val cropIds = listOf(
        "wheat",
        "potato",
        "carrot",
        "pumpkin",
        "melon",
        "cocoa_beans",
        "sugar_cane",
        "cactus",
        "nether_wart",
        "red_mushroom",
        "brown_mushroom",
        "moonflower",
        "sunflower",
        "wild_rose",
        "fire",
        "dead_plant",
        "fermento",
    )

    private val mutationIds = listOf(
        "ashwreath",
        "choconut",
        "dustgrain",
        "gloomgourd",
        "lonelily",
        "scourroot",
        "shadevine",
        "veilshroom",
        "witherbloom",
        "chocoberry",
        "cindershade",
        "coalroot",
        "creambloom",
        "duskbloom",
        "thornshade",
        "blastberry",
        "cheesebite",
        "chloronite",
        "do_not_eat_shroom",
        "fleshtrap",
        "magic_jellybean",
        "noctilume",
        "snoozling",
        "soggybud",
        "chorus_fruit",
        "plantboy_advance",
        "puffercloud",
        "shellfruit",
        "startlevine",
        "stoplight_petal",
        "thunderling",
        "turtlellini",
        "zombud",
        "all_in_aloe",
        "devourer",
        "glasscorn",
        "godseed",
        "jerryflower",
        "phantomleaf",
        "timestalk",
    )

    private val allIds = cropIds + mutationIds
}
