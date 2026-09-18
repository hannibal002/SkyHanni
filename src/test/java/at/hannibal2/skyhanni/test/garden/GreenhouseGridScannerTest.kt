package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.CropCategory
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseCropScanner
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseDetectedMutation
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseGridScanner
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseMutation
import at.hannibal2.skyhanni.utils.LorenzVec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GreenhouseGridScannerTest {

    @Test
    fun `multi-cell mutations reserve their complete planting-grid footprint`() {
        val cells = GreenhouseGridScanner.occupiedMutationCells(
            LorenzVec(100, 70, 200),
            listOf(
                GreenhouseDetectedMutation(GreenhouseMutation.GLASSCORN, LorenzVec(100, 74, 200), ""),
            ),
        )

        assertEquals(setOf(4 to 4, 4 to 5, 5 to 4, 5 to 5), cells)
    }

    @Test
    fun `mutation footprints are clipped to the ten by ten planting grid`() {
        val cells = GreenhouseGridScanner.occupiedMutationCells(
            LorenzVec(100, 70, 200),
            listOf(
                GreenhouseDetectedMutation(GreenhouseMutation.GODSEED, LorenzVec(95, 74, 195), ""),
            ),
        )

        assertEquals(setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), cells)
    }

    @Test
    fun `layout crop ids use the same categories as unique detection`() {
        assertEquals(CropCategory.WILD_ROSE, CropCategory.fromCropId("wild_rose"))
        assertEquals(CropCategory.SUNFLOWER, CropCategory.fromCropId("moonflower"))
        assertEquals(CropCategory.MUSHROOM, CropCategory.fromCropId("brown_mushroom"))
    }

    @Test
    fun `wild rose wins when decorative wheat occupies the same crop cell`() {
        assertEquals("wild_rose", GreenhouseCropScanner.preferredCropId(listOf("wheat", "wild_rose")))
        assertEquals("wild_rose", GreenhouseCropScanner.preferredCropId(listOf("wild_rose", "wheat")))
    }

    @Test
    fun `only positions inside the ten by ten planting grid are accepted`() {
        val middle = LorenzVec(100, 70, 200)

        assertTrue(GreenhouseGridScanner.isInsideGrid(middle, LorenzVec(95, 74, 195)))
        assertTrue(GreenhouseGridScanner.isInsideGrid(middle, LorenzVec(104, 74, 204)))
        assertFalse(GreenhouseGridScanner.isInsideGrid(middle, LorenzVec(105, 74, 200)))
        assertFalse(GreenhouseGridScanner.isInsideGrid(middle, LorenzVec(100, 74, 205)))
    }
}
