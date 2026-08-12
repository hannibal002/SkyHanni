package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.SkyMutationsLayoutCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkyMutationsLayoutCodecTest {

    @Test
    fun `decode SkyMutations greenhouse link`() {
        val layout = SkyMutationsLayoutCodec.decode(FIXTURE)

        assertTrue(layout.inputs.isNotEmpty())
        assertTrue(layout.targets.isNotEmpty())
        assertTrue(layout.placements.all { it.row in 0..9 && it.column in 0..9 })
        assertEquals(layout.placements.size, layout.placements.map { it.row to it.column }.toSet().size)
    }

    companion object {
        private const val FIXTURE = "https://skymutations.eu/greenhouse?layout=" +
            "NrAsBoIIgFQCwPYCcB2BnOBDAJgUyuAAwC64wAzOJVAGq4CWANhkgggLYECMpFk4UAOpNsAAgBKCNPnA8ylAKwC6" +
            "TFm06zeEasMZjJ07lvBKhIiVJlzgSneYNXeS6CuZxWHI2SWnXaz5rEQA"
    }
}
