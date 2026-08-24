package at.hannibal2.skyhanni.features.garden.greenhouse

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal object SkyLayoutsLayoutCodec {

    fun decode(input: String): SkyShardsLayoutCodec.Layout {
        require(input.contains(SKYLAYOUTS_HOST, ignoreCase = true)) {
            "The clipboard does not contain a SkyLayouts link."
        }
        val plot = plotQuery.find(input)?.groupValues?.get(1)?.toInt() ?: 1
        val encodedBoard = boardPattern.findAll(input)
            .firstOrNull { it.groupValues[1].toInt() == plot }
            ?.groupValues?.get(2)
            ?: throw IllegalArgumentException("The SkyLayouts link does not contain Plot $plot data.")
        val board = URLDecoder.decode(encodedBoard, StandardCharsets.UTF_8)
        val parts = board.split('~')
        require(parts.size == BOARD_SECTIONS && parts[0] == FORMAT_VERSION) {
            "The SkyLayouts link uses an unsupported layout format."
        }
        val gridSize = parts[1].toIntOrNull(RADIX)
        require(gridSize == GRID_SIZE) { "SkyHanni only supports 10 by 10 SkyLayouts plots." }

        val palette = parts[2].split(',').map(::normalizeId)
        require(palette.isNotEmpty() && palette.none(String::isEmpty)) {
            "The SkyLayouts layout has an invalid crop palette."
        }
        val placements = mutableListOf<SkyShardsLayoutCodec.Placement>()
        var cellIndex = 0
        var tokenIndex = 0
        val grid = parts[3]
        while (tokenIndex < grid.length) {
            val token = grid[tokenIndex++]
            if (token == EMPTY_CELL) {
                val runToken = grid.getOrNull(tokenIndex++)
                    ?: throw IllegalArgumentException("The SkyLayouts layout ends with an incomplete empty run.")
                val runLength = runToken.digitToIntOrNull(RADIX)
                    ?: throw IllegalArgumentException("Invalid SkyLayouts empty run: $runToken")
                require(runLength > 0 && cellIndex + runLength <= GRID_CELLS) {
                    "The SkyLayouts layout contains an invalid empty run."
                }
                cellIndex += runLength
                continue
            }
            require(cellIndex < GRID_CELLS) { "The SkyLayouts layout contains too many cells." }
            val paletteIndex = token.digitToIntOrNull(RADIX)
                ?: throw IllegalArgumentException("Invalid SkyLayouts cell token: $token")
            val cropId = palette.getOrNull(paletteIndex)
                ?: throw IllegalArgumentException("Unknown SkyLayouts palette index: $token")
            placements.add(
                SkyShardsLayoutCodec.Placement(
                    cropId = cropId,
                    row = cellIndex / GRID_SIZE,
                    column = cellIndex % GRID_SIZE,
                    target = false,
                ),
            )
            cellIndex++
        }
        require(cellIndex == GRID_CELLS) {
            "Invalid SkyLayouts grid: expected $GRID_CELLS cells, but decoded $cellIndex."
        }
        return SkyShardsLayoutCodec.Layout(placements)
    }

    private fun normalizeId(id: String): String = id.lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')

    private val plotQuery = "[?&]p=([123])(?:[&#]|$)".toRegex(RegexOption.IGNORE_CASE)
    private val boardPattern = "(?:#|&)b([123])=([^&#\\s]+)".toRegex(RegexOption.IGNORE_CASE)
    private const val SKYLAYOUTS_HOST = "skylayouts.io"
    private const val FORMAT_VERSION = "2"
    private const val BOARD_SECTIONS = 4
    private const val GRID_SIZE = 10
    private const val GRID_CELLS = GRID_SIZE * GRID_SIZE
    private const val RADIX = 36
    private const val EMPTY_CELL = '.'
}
