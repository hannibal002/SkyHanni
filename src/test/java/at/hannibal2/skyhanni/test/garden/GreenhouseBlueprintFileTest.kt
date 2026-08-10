package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.GardenStorage.GreenHouseStorage
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseBlueprintFile
import at.hannibal2.skyhanni.utils.LorenzVec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GreenhouseBlueprintFileTest {

    @Test
    fun `layout files round trip portable blueprint data`() {
        val blueprint = GreenHouseStorage.MutationBlueprintStorage(
            minXOffset = -5,
            minZOffset = -5,
            maxXOffset = 4,
            maxZOffset = 4,
            mutations = mutableListOf(
                GreenHouseStorage.MutationPlacementStorage(
                    mutationId = "CHLORONITE",
                    offset = LorenzVec(1.0, 64.0, -2.0),
                    texture = "untrusted-texture-value",
                    size = 1,
                ),
            ),
            importedCells = mutableListOf(
                GreenHouseStorage.BlueprintCellStorage("wheat", row = 2, column = 3),
            ),
            targetMutationId = "CHORUS_FRUIT",
        )

        val serialized = GreenhouseBlueprintFile.encode("Chorus Setup", blueprint)
        val imported = GreenhouseBlueprintFile.decode(serialized)

        assertFalse(serialized.contains("untrusted-texture-value"))
        assertEquals("Chorus Setup", imported.name)
        assertEquals("CHORUS_FRUIT", imported.blueprint.targetMutationId)
        assertEquals("CHLORONITE", imported.blueprint.mutations.single().mutationId)
        assertEquals(LorenzVec(1.0, 64.0, -2.0), imported.blueprint.mutations.single().offset)
        assertEquals("", imported.blueprint.mutations.single().texture)
        assertEquals("wheat", imported.blueprint.importedCells.single().cropId)
        assertEquals(2, imported.blueprint.importedCells.single().row)
        assertEquals(3, imported.blueprint.importedCells.single().column)
    }

    @Test
    fun `unrelated json files are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GreenhouseBlueprintFile.decode("{\"format\":\"something-else\",\"version\":1}")
        }
    }
}
