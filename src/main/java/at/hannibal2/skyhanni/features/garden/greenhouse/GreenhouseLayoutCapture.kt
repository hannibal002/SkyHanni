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
        }
        GreenhouseGridScanner.scanCropCells(plot, mutations).forEach { cell ->
            add(GreenHouseStorage.BlueprintCellStorage(cell.cropId, cell.row, cell.column))
        }
    }

    private const val GRID_RADIUS = GreenhouseGridScanner.GRID_RADIUS
}

internal data class GreenhouseDetectedMutation(
    val mutation: GreenhouseMutation,
    val position: LorenzVec,
    val texture: String,
)
