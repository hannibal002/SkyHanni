package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.GardenStorage.GreenHouseStorage
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.utils.LorenzVec
import kotlin.math.floor

internal object GreenhouseLayoutCapture {

    fun captureCells(
        plot: GardenPlot,
        mutations: List<GreenhouseDetectedMutation>,
    ): List<GreenHouseStorage.BlueprintCellStorage> = buildList {
        val middle = plot.middle.toBlockPos()
        val occupiedByMutations = mutableSetOf<Pair<Int, Int>>()
        mutations.forEach { detected ->
            val anchorColumn = floor(detected.position.x).toInt() - middle.x + GRID_RADIUS
            val anchorRow = floor(detected.position.z).toInt() - middle.z + GRID_RADIUS
            val topLeftColumn = anchorColumn - detected.mutation.size / 2
            val topLeftRow = anchorRow - detected.mutation.size / 2
            add(
                GreenHouseStorage.BlueprintCellStorage(
                    cropId = detected.mutation.internalId.lowercase(),
                    row = topLeftRow,
                    column = topLeftColumn,
                ),
            )
            repeat(detected.mutation.size) { rowOffset ->
                repeat(detected.mutation.size) { columnOffset ->
                    occupiedByMutations.add(topLeftRow + rowOffset to topLeftColumn + columnOffset)
                }
            }
        }
        for (row in 0 until GRID_SIZE) {
            for (column in 0 until GRID_SIZE) {
                if (row to column in occupiedByMutations) continue
                val position = LorenzVec(
                    middle.x + column - GRID_RADIUS.toDouble(),
                    CROP_Y,
                    middle.z + row - GRID_RADIUS.toDouble(),
                )
                GreenhouseCropScanner.skyShardsCropIdAt(position)?.let { cropId ->
                    add(GreenHouseStorage.BlueprintCellStorage(cropId = cropId, row = row, column = column))
                }
            }
        }
    }

    private const val GRID_RADIUS = 5
    private const val GRID_SIZE = GRID_RADIUS * 2
    private const val CROP_Y = 74.0
}

internal data class GreenhouseDetectedMutation(
    val mutation: GreenhouseMutation,
    val position: LorenzVec,
    val texture: String,
)
