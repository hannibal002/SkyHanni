package at.hannibal2.skyhanni.features.garden.greenhouse

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Inflater

object SkyShardsDecoder {

    private const val GRID_SIZE = 10
    private const val TOTAL_CELLS = GRID_SIZE * GRID_SIZE
    private const val LETTERS = "abcdefghijklmnopqrstuvwxyz"

    val CROP_IDS = listOf(
        "wheat", "potato", "carrot", "pumpkin", "melon", "cocoa_beans", "sugar_cane",
        "cactus", "nether_wart", "red_mushroom", "brown_mushroom", "moonflower", "sunflower", "wild_rose",
        "fire", "dead_plant", "fermento",
    )
    val MUTATION_IDS = listOf(
        "ashwreath", "choconut", "dustgrain", "gloomgourd", "lonelily", "scourroot", "shadevine",
        "veilshroom", "witherbloom", "chocoberry", "cindershade", "coalroot", "creambloom", "duskbloom",
        "thornshade", "blastberry", "cheesebite", "chloronite", "do_not_eat_shroom", "fleshtrap", "magic_jellybean",
        "noctilume", "snoozling", "soggybud", "chorus_fruit", "plantboy_advance", "puffercloud", "shellfruit",
        "startlevine", "stoplight_petal", "thunderling", "turtlellini", "zombud", "all_in_aloe", "devourer",
        "glasscorn", "godseed", "jerryflower", "phantomleaf", "timestalk",
    )

    data class Placement(
        val cropId: String,
        val position: GridPosition,
    )

    data class DecodedDesign(
        val inputs: List<Placement>,
        val targets: List<Placement>,
    )

    fun decodeDesign(input: String): DecodedDesign? {
        val parts = inflateRaw(fromUrlSafeBase64(input)).split("|")
        if (parts.size != 3) return null

        val (inputIndexString, targetIndexString, grid) = parts

        val inputCrops = parseCropList(inputIndexString)
        val targetCrops = parseCropList(targetIndexString)

        val useDouble = grid.length == TOTAL_CELLS * 2
        val charWidth = if (useDouble) 2 else 1
        val empty = if (useDouble) ".." else "."

        val inputs = mutableListOf<Placement>()
        val targets = mutableListOf<Placement>()

        repeat(TOTAL_CELLS) { pos ->
            val chars = grid.substring(pos * charWidth, (pos + 1) * charWidth)

            if (chars == empty) return@repeat

            val index = charsToIndex(chars, useDouble)
            if (index == -1) return@repeat

            val placement = Placement(
                cropId = when {
                    chars.isUpperCase() -> targetCrops.getOrNull(index)
                    else -> inputCrops.getOrNull(index)
                } ?: return@repeat,
                position = GridPosition(pos / GRID_SIZE, pos % GRID_SIZE),
            )

            if (chars.isUpperCase()) {
                targets += placement
            } else {
                inputs += placement
            }
        }

        return DecodedDesign(inputs, targets)
    }

    private fun parseCropList(data: String): List<String> =
        data.takeIf(String::isNotEmpty)
            ?.split(",")
            ?.mapNotNull(::indexToCrop)
            .orEmpty()

    private fun indexToCrop(indexString: String): String? {
        val index = indexString.toInt(36)

        return when {
            index < CROP_IDS.size -> CROP_IDS[index]
            else -> MUTATION_IDS.getOrNull(index - CROP_IDS.size)
        }
    }

    private fun charsToIndex(chars: String, useDouble: Boolean): Int =
        if (useDouble) {
            doubleToIndex(chars)
        } else {
            LETTERS.indexOf(chars.lowercase())
        }

    private fun String.isUpperCase(): Boolean =
        this == uppercase() && this != lowercase()

    private fun fromUrlSafeBase64(str: String): ByteArray {
        var base64 = str
            .replace('-', '+')
            .replace('_', '/')

        while (base64.length % 4 != 0) {
            base64 += "="
        }

        return Base64.getDecoder().decode(base64)
    }

    private fun inflateRaw(data: ByteArray): String {
        val inflater = Inflater(true)
        inflater.setInput(data)

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)

        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            output.write(buffer, 0, count)
        }

        inflater.end()

        return output.toString(Charsets.UTF_8.name())
    }

    private fun doubleToIndex(chars: String): Int {
        val firstIdx = LETTERS.indexOf(chars[0].lowercaseChar())
        val secondIdx = LETTERS.indexOf(chars[1].lowercaseChar())

        if (firstIdx == -1 || secondIdx == -1) return -1

        return firstIdx * LETTERS.length + secondIdx
    }
}
