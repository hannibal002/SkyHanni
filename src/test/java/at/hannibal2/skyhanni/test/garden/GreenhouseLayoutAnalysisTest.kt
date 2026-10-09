package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutAnalysis
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseMutation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GreenhouseLayoutAnalysisTest {

    @Test
    fun `chorus setup is inferred with or without an already spawned chorus fruit`() {
        val setup = buildList {
            repeat(5) { add(GreenhouseLayoutAnalysis.Entry("chloronite")) }
            repeat(3) { add(GreenhouseLayoutAnalysis.Entry("magic_jellybean")) }
        }
        assertEquals(GreenhouseMutation.CHORUS_FRUIT, GreenhouseLayoutAnalysis.inferTarget(setup))
        assertEquals(
            GreenhouseMutation.CHORUS_FRUIT,
            GreenhouseLayoutAnalysis.inferTarget(setup + GreenhouseLayoutAnalysis.Entry("chorus_fruit")),
        )
    }

    @Test
    fun `spawned output is ignored while setup buffs and unique crops keep their roles`() {
        val target = GreenhouseMutation.CHORUS_FRUIT
        assertEquals(
            GreenhouseLayoutAnalysis.Role.TARGET_OUTPUT,
            GreenhouseLayoutAnalysis.roleFor(GreenhouseLayoutAnalysis.Entry("chorus_fruit"), target),
        )
        assertEquals(
            GreenhouseLayoutAnalysis.Role.SPAWN_INPUT,
            GreenhouseLayoutAnalysis.roleFor(GreenhouseLayoutAnalysis.Entry("chloronite"), target),
        )
        assertEquals(
            GreenhouseLayoutAnalysis.Role.YIELD_BUFF,
            GreenhouseLayoutAnalysis.roleFor(GreenhouseLayoutAnalysis.Entry("dustgrain"), target),
        )
        assertEquals(
            GreenhouseLayoutAnalysis.Role.UNIQUE_CROP,
            GreenhouseLayoutAnalysis.roleFor(GreenhouseLayoutAnalysis.Entry("wheat"), target),
        )
    }
}
