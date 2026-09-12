package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.features.fishing.WormholeFinder
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.core.particles.ParticleTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WormholeFinderTest {

    @Test
    fun `prefers closer wormhole when direction scores are nearly tied`() {
        val origin = LorenzVec()
        val close = wormhole(1, 10.0, 0.0, 0.5)
        val far = wormhole(2, 100.0, 0.0, 0.0)

        val found = WormholeFinder.findBestWormhole(sequenceOf(far, close), origin, LorenzVec(1, 0, 0))

        assertEquals(close, found)
    }

    @Test
    fun `does not match wormhole behind arrow`() {
        val found = WormholeFinder.findBestWormhole(
            sequenceOf(wormhole(1, -10.0, 0.0, 0.0)),
            LorenzVec(),
            LorenzVec(1, 0, 0),
        )

        assertNull(found)
    }

    @Test
    fun `ignores wormhole with no horizontal direction from arrow`() {
        val found = WormholeFinder.findBestWormhole(
            sequenceOf(wormhole(1, 0.0, 10.0, 0.0)),
            LorenzVec(),
            LorenzVec(1, 0, 0),
        )

        assertNull(found)
    }

    @Test
    fun `detects wormhole particle signature`() {
        assertTrue(WormholeFinder.isWormholeParticle(ParticleTypes.ENCHANT, count = 4, speed = -1.2f))
        assertTrue(WormholeFinder.isWormholeParticle(ParticleTypes.PORTAL, count = 5, speed = 0.25f))

        assertFalse(WormholeFinder.isWormholeParticle(ParticleTypes.BUBBLE, count = 1, speed = 0f))
        assertFalse(WormholeFinder.isWormholeParticle(ParticleTypes.DUST, count = 0, speed = 1f))
    }

    private fun wormhole(id: Int, x: Double, y: Double, z: Double) = GraphNode(
        id = id,
        position = LorenzVec(x, y, z),
        name = "Wormhole",
        tagNames = listOf(GraphNodeTag.FISHING_WORMHOLE.internalName),
    )
}
