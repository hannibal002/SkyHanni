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

        assertEquals(58, layout.inputs.size)
        assertEquals(0, layout.targets.size)
        assertEquals(9, layout.inputs.count { it.cropId == "noctilume" })
        assertEquals(41, layout.inputs.count { it.cropId == "soggybud" })
        assertEquals(2, layout.inputs.count { it.cropId == "cindershade" })
        assertEquals(1, layout.inputs.count { it.cropId == "wild_rose" })
    }

    @Test
    fun `compressed Noctilume layout contains nine plants instead of thirty-six`() {
        val layout = SkyLayoutsLayoutCodec.decode(
            "https://skylayouts.io/l/1AB~zKHPNf3qekLc01uFltErY3bDoAwCENL1znv_v_fGpjjwfhoszTZoVAji0RK1Q0kugVTMkGDuXcW_2A-fa1NZLOZxNKyw_Le-ksHyFI376ga2ntQED9kOS25kYebJxTPhSg4zuuJ3Q",
        )
        assertEquals(
            listOf(0 to 1, 0 to 6, 2 to 1, 2 to 6, 4 to 6, 5 to 2, 6 to 6, 8 to 1, 8 to 6),
            layout.inputs.filter { it.cropId == "noctilume" }.map { it.row to it.column },
        )
    }

    @Test
    fun `touching three by three plants retain separate top-left placements`() {
        val cells = MutableList<String?>(100) { null }
        for (row in 0..2) {
            for (column in 0..5) cells[row * 10 + column] = "SNOOZLING"
        }
        val layout = SkyLayoutsLayoutCodec.layoutFromCells(cells)
        assertEquals(listOf(0 to 0, 0 to 3), layout.inputs.map { it.row to it.column })
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
