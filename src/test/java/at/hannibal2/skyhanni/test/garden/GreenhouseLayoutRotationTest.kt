package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.GardenStorage.GreenHouseStorage
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseMutationBlueprint
import at.hannibal2.skyhanni.utils.LorenzVec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GreenhouseLayoutRotationTest {

    @Test
    fun `rotates cells and mutation anchors clockwise`() {
        val blueprint = GreenHouseStorage.MutationBlueprintStorage(
            minXOffset = -5,
            minZOffset = -5,
            maxXOffset = 4,
            maxZOffset = 4,
            mutations = mutableListOf(
                GreenHouseStorage.MutationPlacementStorage("WHEAT", LorenzVec(-2, 3, -1), size = 1),
                GreenHouseStorage.MutationPlacementStorage("GLASSCORN", LorenzVec(-1, 4, -2), size = 2),
                GreenHouseStorage.MutationPlacementStorage("GODSEED", LorenzVec(2, 5, 1), size = 3),
            ),
            importedCells = mutableListOf(
                GreenHouseStorage.BlueprintCellStorage("wheat", row = 2, column = 3),
                GreenHouseStorage.BlueprintCellStorage("glasscorn", row = 2, column = 3),
                GreenHouseStorage.BlueprintCellStorage("godseed", row = 1, column = 4),
            ),
        )

        GreenhouseMutationBlueprint.rotateLayoutClockwise(blueprint)

        assertEquals(LorenzVec(0, 3, -2), blueprint.mutations[0].offset)
        assertEquals(LorenzVec(2, 4, -1), blueprint.mutations[1].offset)
        assertEquals(LorenzVec(-2, 5, 2), blueprint.mutations[2].offset)
        assertEquals(3 to 7, blueprint.importedCells[0].let { it.row to it.column })
        assertEquals(3 to 6, blueprint.importedCells[1].let { it.row to it.column })
        assertEquals(4 to 6, blueprint.importedCells[2].let { it.row to it.column })
    }

    @Test
    fun `four rotations restore the original layout`() {
        val blueprint = GreenHouseStorage.MutationBlueprintStorage(
            minXOffset = -5,
            minZOffset = -5,
            maxXOffset = 4,
            maxZOffset = 4,
            mutations = mutableListOf(
                GreenHouseStorage.MutationPlacementStorage("GLASSCORN", LorenzVec(-1, 4, -2), size = 2),
            ),
            importedCells = mutableListOf(
                GreenHouseStorage.BlueprintCellStorage("glasscorn", row = 2, column = 3),
            ),
        )

        repeat(4) { GreenhouseMutationBlueprint.rotateLayoutClockwise(blueprint) }

        assertEquals(LorenzVec(-1, 4, -2), blueprint.mutations.single().offset)
        assertEquals(2 to 3, blueprint.importedCells.single().let { it.row to it.column })
        assertEquals(listOf(-5, -5, 4, 4), listOf(
            blueprint.minXOffset,
            blueprint.minZOffset,
            blueprint.maxXOffset,
            blueprint.maxZOffset,
        ))
    }
}
