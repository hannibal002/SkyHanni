package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.SkyLayoutsLayoutCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SkyLayoutsLayoutCodecTest {

    @Test
    fun `decode Markdown escaped SkyLayouts short link`() {
        val escapedUrl = SHORT_FIXTURE.replace("~", "\\~").replace("_", "\\_")
        val escaped = "[$escapedUrl]($escapedUrl)"

        val layout = SkyLayoutsLayoutCodec.decode(escaped)

        assertEquals(SkyLayoutsLayoutCodec.decode(SHORT_FIXTURE), layout)
    }

    @Test
    fun `decode SkyLayouts short link`() {
        val layout = SkyLayoutsLayoutCodec.decode(SHORT_FIXTURE)

        assertEquals(85, layout.inputs.size)
        assertEquals(0, layout.targets.size)
        assertEquals(36, layout.inputs.count { it.cropId == "noctilume" })
        assertEquals(41, layout.inputs.count { it.cropId == "soggybud" })
        assertEquals(2, layout.inputs.count { it.cropId == "cindershade" })
        assertEquals(1, layout.inputs.count { it.cropId == "wild_rose" })
    }

    @Test
    fun `reject old SkyLayouts link`() {
        assertThrows(IllegalArgumentException::class.java) {
            SkyLayoutsLayoutCodec.decode("https://skylayouts.io/layout?p=1#b1=2~a~WHEAT~0")
        }
    }

    companion object {
        private const val SHORT_FIXTURE = "https://skylayouts.io/l/" +
            "1xH~p3KIqfL0Nel1j3KPCnhU1uiEJNH19SDEVhl6WM7Z9rvv-rOPphqBFaOqG-DL-LAQ7"
    }
}
