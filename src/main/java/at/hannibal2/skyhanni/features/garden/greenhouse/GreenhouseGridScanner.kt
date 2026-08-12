package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.phys.AABB
import kotlin.math.floor

/** Classifies only the Greenhouse's 10x10 planting grid. */
internal object GreenhouseGridScanner {

    data class CropCell(
        val cropId: String,
        val category: CropCategory,
        val row: Int,
        val column: Int,
        val position: LorenzVec,
    )

    data class Scan(
        val mutations: List<GreenhouseDetectedMutation>,
        val crops: List<CropCell>,
    ) {
        val cropPositions: Map<CropCategory, LorenzVec>
            get() = buildMap {
                crops.forEach { putIfAbsent(it.category, it.position) }
            }
    }

    fun scan(plot: GardenPlot): Scan {
        val mutations = GreenhouseMutationScanner.scan(area(plot))
        return Scan(mutations, scanCropCells(plot, mutations))
    }

    fun scanCropCells(
        plot: GardenPlot,
        mutations: List<GreenhouseDetectedMutation>,
    ): List<CropCell> = buildList {
        val middle = plot.middle.toBlockPos()
        val occupiedByMutations = occupiedMutationCells(plot.middle, mutations)
        for (row in 0 until GRID_SIZE) {
            for (column in 0 until GRID_SIZE) {
                if (row to column in occupiedByMutations) continue
                val position = LorenzVec(
                    middle.x + column - GRID_RADIUS.toDouble(),
                    CROP_Y,
                    middle.z + row - GRID_RADIUS.toDouble(),
                )
                val cropId = GreenhouseCropScanner.skyShardsCropIdAt(position) ?: continue
                val category = CropCategory.fromCropId(cropId) ?: continue
                add(CropCell(cropId, category, row, column, position))
            }
        }
    }

    internal fun occupiedMutationCells(
        plotMiddle: LorenzVec,
        mutations: List<GreenhouseDetectedMutation>,
    ): Set<Pair<Int, Int>> = buildSet {
        val middle = plotMiddle.toBlockPos()
        mutations.forEach { detected ->
            val anchorColumn = floor(detected.position.x).toInt() - middle.x + GRID_RADIUS
            val anchorRow = floor(detected.position.z).toInt() - middle.z + GRID_RADIUS
            val topLeftColumn = anchorColumn - detected.mutation.size / 2
            val topLeftRow = anchorRow - detected.mutation.size / 2
            repeat(detected.mutation.size) { rowOffset ->
                repeat(detected.mutation.size) { columnOffset ->
                    val row = topLeftRow + rowOffset
                    val column = topLeftColumn + columnOffset
                    if (row in 0 until GRID_SIZE && column in 0 until GRID_SIZE) add(row to column)
                }
            }
        }
    }

    fun isInsideGrid(plot: GardenPlot, position: LorenzVec): Boolean =
        isInsideGrid(plot.middle, position)

    internal fun isInsideGrid(plotMiddle: LorenzVec, position: LorenzVec): Boolean {
        val middle = plotMiddle.toBlockPos()
        val block = position.toBlockPos()
        return block.x in middle.x - GRID_RADIUS until middle.x + GRID_RADIUS &&
            block.z in middle.z - GRID_RADIUS until middle.z + GRID_RADIUS
    }

    fun area(plot: GardenPlot): AABB {
        val middle = plot.middle.toBlockPos()
        return AABB(
            (middle.x - GRID_RADIUS).toDouble(),
            MIN_GARDEN_Y.toDouble(),
            (middle.z - GRID_RADIUS).toDouble(),
            (middle.x + GRID_RADIUS).toDouble(),
            MAX_GARDEN_Y.toDouble() + 1,
            (middle.z + GRID_RADIUS).toDouble(),
        )
    }

    fun isLoaded(plot: GardenPlot): Boolean = area(plot).let { area ->
        listOf(
            LorenzVec(area.minX, MIN_GARDEN_Y.toDouble(), area.minZ),
            LorenzVec(area.minX, MIN_GARDEN_Y.toDouble(), area.maxZ - 1),
            LorenzVec(area.maxX - 1, MIN_GARDEN_Y.toDouble(), area.minZ),
            LorenzVec(area.maxX - 1, MIN_GARDEN_Y.toDouble(), area.maxZ - 1),
        ).all { it.isInLoadedChunk() }
    }

    const val GRID_RADIUS = 5
    private const val GRID_SIZE = GRID_RADIUS * 2
    private const val CROP_Y = 74.0
    private const val MIN_GARDEN_Y = 60
    private const val MAX_GARDEN_Y = 100
}
