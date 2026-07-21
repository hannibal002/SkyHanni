package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi.GridPosition
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

    val sizedMutations = mapOf(
        "noctilume" to 2,
        "snoozling" to 3,
        "plantboy_advance" to 2,
        "glasscorn" to 2,
        "godseed" to 3,
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
        val decodedBytes = fromUrlSafeBase64OrNull(input) ?: return null
        val decompressedRaw = inflateRawOrNull(decodedBytes) ?: return null

        val parts = decompressedRaw.split("|")
        if (parts.size != 3) return null

        val (inputIndexString, targetIndexString, grid) = parts

        val useDouble = grid.length == TOTAL_CELLS * 2
        val charWidth = if (useDouble) 2 else 1

        // Prevent StringIndexOutOfBoundsException if the layout string has corrupted size metrics
        if (grid.length < TOTAL_CELLS * charWidth) return null

        val empty = if (useDouble) ".." else "."

        val inputCrops = parseCropList(inputIndexString)
        val targetCrops = parseCropList(targetIndexString)

        val inputs = mutableListOf<Placement>()
        val targets = mutableListOf<Placement>()

        repeat(TOTAL_CELLS) { pos ->
            val chars = grid.substring(pos * charWidth, (pos + 1) * charWidth)
            if (chars == empty) return@repeat

            val index = charsToIndex(chars, useDouble)
            if (index == -1) return@repeat

            val isTarget = chars.isUpperCase()
            val cropId = if (isTarget) targetCrops.getOrNull(index) else inputCrops.getOrNull(index)
            if (cropId == null) return@repeat

            val targetList = if (isTarget) targets else inputs
            val posX = pos % GRID_SIZE
            val posY = pos / GRID_SIZE

            val size = sizedMutations[cropId]
            if (size != null) {
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        targetList.add(Placement(cropId = cropId, position = GridPosition(posX + x, posY + y)))
                    }
                }
            } else {
                targetList.add(Placement(cropId = cropId, position = GridPosition(posX, posY)))
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
        // Safe numerical casting eliminates unexpected format exceptions
        val index = indexString.toIntOrNull(36) ?: return null

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

    private fun fromUrlSafeBase64OrNull(str: String): ByteArray? = runCatching {
        // Native URL decoding handles url structures natively and fast
        Base64.getUrlDecoder().decode(str)
    }.getOrNull() ?: runCatching {
        // Safe legacy fallback translation layer
        var base64 = str.replace('-', '+').replace('_', '/')
        while (base64.length % 4 != 0) {
            base64 += "="
        }
        Base64.getDecoder().decode(base64)
    }.getOrNull()

    private fun inflateRawOrNull(data: ByteArray): String? = runCatching {
        val inflater = Inflater(true)
        try {
            inflater.setInput(data)
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0 && inflater.needsInput()) break
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
        } finally {
            // Releasing the decompression memory context under any status prevents heap leaks
            inflater.end()
        }
    }.getOrNull()

    private fun doubleToIndex(chars: String): Int {
        if (chars.length < 2) return -1
        val firstIdx = LETTERS.indexOf(chars[0].lowercaseChar())
        val secondIdx = LETTERS.indexOf(chars[1].lowercaseChar())

        if (firstIdx == -1 || secondIdx == -1) return -1

        return firstIdx * LETTERS.length + secondIdx
    }
}
