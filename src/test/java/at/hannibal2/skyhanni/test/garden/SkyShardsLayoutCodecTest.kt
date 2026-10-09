package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.SkyShardsLayoutCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SkyShardsLayoutCodecTest {

    @Test
    fun `decode current SkyShards share format`() {
        val layout = SkyShardsLayoutCodec.decode("https://api.skyshards.com/share/$FIXTURE")

        assertEquals(47, layout.inputs.size)
        assertEquals(0, layout.targets.size)
        assertEquals(25, layout.inputs.count { it.cropId == "chloronite" })
        assertEquals(15, layout.inputs.count { it.cropId == "magic_jellybean" })
        assertEquals(7, layout.inputs.count { it.cropId == "wheat" })
    }

    @Test
    fun `extract code from designer query link`() {
        val layout = SkyShardsLayoutCodec.decode("https://greenhouse.skyshards.com/designer?layout=$FIXTURE")

        assertEquals(47, layout.inputs.size)
        assertEquals(0, layout.targets.size)
    }

    companion object {
        private const val FIXTURE = "q9QxNNQxqKnRQ4BEIEhKTNRL1ksCQxALJADEIBZUTA8kBoIgFlQlVB1YLxwkw0kA"
    }
}
