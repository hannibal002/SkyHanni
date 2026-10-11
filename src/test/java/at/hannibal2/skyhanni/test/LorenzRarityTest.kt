package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.LorenzRarity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LorenzRarityTest {

    // Pinned against the NEU repo, where PHOENIX;7 is Special. An entry inserted or moved before VERY_SPECIAL breaks this.
    // ULTIMATE is left out, no NEU pet uses it.
    private val neuPetIndices = mapOf(
        LorenzRarity.COMMON to 0,
        LorenzRarity.UNCOMMON to 1,
        LorenzRarity.RARE to 2,
        LorenzRarity.EPIC to 3,
        LorenzRarity.LEGENDARY to 4,
        LorenzRarity.MYTHIC to 5,
        LorenzRarity.DIVINE to 6,
        LorenzRarity.SPECIAL to 7,
        LorenzRarity.VERY_SPECIAL to 8,
    )

    @Test
    fun `neu pet indices match the NEU repo`() {
        for ((rarity, index) in neuPetIndices) {
            Assertions.assertEquals(index, rarity.neuPetIndex, "neuPetIndex of $rarity")
            Assertions.assertEquals(rarity, LorenzRarity.fromNeuPetIndex(index), "fromNeuPetIndex($index)")
        }
    }

    @Test
    fun `fromNeuPetIndex returns null out of range`() {
        Assertions.assertNull(LorenzRarity.fromNeuPetIndex(-1))
        Assertions.assertNull(LorenzRarity.fromNeuPetIndex(LorenzRarity.entries.size))
    }
}
