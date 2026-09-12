package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseMutation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GreenhouseMutationTest {

    @Test
    fun `catalogue contains every official mutation id`() {
        assertEquals(40, GreenhouseMutation.entries.size)
        assertEquals(40, GreenhouseMutation.entries.map { it.internalId }.toSet().size)
    }

    @Test
    fun `armor stand aliases resolve to canonical mutation ids`() {
        mapOf(
            "snoozlingFlower" to "SNOOZLING",
            "Lonelilly" to "LONELILY",
            "Plantboy" to "PLANTBOY_ADVANCE",
            "jerryseed" to "JERRYFLOWER",
            "Do-not-eat-shroom" to "DO_NOT_EAT_SHROOM",
        ).forEach { (name, expectedId) ->
            assertEquals(expectedId, GreenhouseMutation.fromName(name)?.internalId)
        }
    }

    @Test
    fun `unrelated armor stand names are ignored`() {
        assertNull(GreenhouseMutation.fromName("Carpenter"))
        assertNull(GreenhouseMutation.fromName("PlantboyRoots"))
        assertNull(GreenhouseMutation.fromName("godseedPillar"))
    }

    @Test
    fun `SkyShards ids and footprints map to mutations`() {
        assertEquals("MAGIC_JELLYBEAN", GreenhouseMutation.fromSkyShardsId("magic_jellybean")?.internalId)
        assertEquals(2, GreenhouseMutation.fromSkyShardsId("plantboy_advance")?.size)
        assertEquals(3, GreenhouseMutation.fromSkyShardsId("godseed")?.size)
    }

    @Test
    fun `spawn rules and yield buffs are available for layout analysis`() {
        assertEquals(
            mapOf("chloronite" to 5, "magic_jellybean" to 3),
            GreenhouseMutation.CHORUS_FRUIT.spawnRequirements,
        )
        assertEquals(true, GreenhouseMutation.DUSTGRAIN.providesYieldBuff)
        assertEquals(false, GreenhouseMutation.CHLORONITE.providesYieldBuff)
        assertEquals(GreenhouseMutation.CHORUS_FRUIT, GreenhouseMutation.fromQuery("chorus"))
    }
}
