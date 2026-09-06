package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseMutationBlueprint.matchesAnchor
import at.hannibal2.skyhanni.utils.LorenzVec
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GreenhouseMutationMatchingTest {

    private val anchor = LorenzVec(100, 74, 200)

    @Test
    fun `large mutations accept diagonal positions within square tolerance`() {
        assertTrue(LorenzVec(99, 74, 199).matchesAnchor(anchor, 2))
        assertTrue(LorenzVec(101, 74, 201).matchesAnchor(anchor, 2))
        assertTrue(LorenzVec(98.5, 74.0, 198.5).matchesAnchor(anchor, 3))
        assertTrue(LorenzVec(101.5, 74.0, 201.5).matchesAnchor(anchor, 3))
    }

    @Test
    fun `large mutations still reject positions beyond horizontal or vertical tolerance`() {
        for (size in 2..3) {
            assertFalse(LorenzVec(102, 74, 200).matchesAnchor(anchor, size))
            assertFalse(LorenzVec(100, 74, 202).matchesAnchor(anchor, size))
            assertFalse(LorenzVec(100, 78, 200).matchesAnchor(anchor, size))
        }
    }

    @Test
    fun `single cell mutations retain their tight matching tolerance`() {
        assertTrue(anchor.matchesAnchor(anchor, 1))
        assertFalse(LorenzVec(101, 74, 201).matchesAnchor(anchor, 1))
    }
}
