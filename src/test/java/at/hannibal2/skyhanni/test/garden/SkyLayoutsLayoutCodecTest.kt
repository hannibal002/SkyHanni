package at.hannibal2.skyhanni.test.garden

import at.hannibal2.skyhanni.features.garden.greenhouse.SkyLayoutsLayoutCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SkyLayoutsLayoutCodecTest {

    @Test
    fun `decode SkyLayouts share link`() {
        val layout = SkyLayoutsLayoutCodec.decode(FIXTURE)

        assertEquals(72, layout.inputs.size)
        assertEquals(0, layout.targets.size)
        assertEquals(19, layout.inputs.count { it.cropId == "chorus_fruit" })
        assertEquals(24, layout.inputs.count { it.cropId == "shellfruit" })
        assertEquals(23, layout.inputs.count { it.cropId == "stoplight_petal" })
        assertEquals("wild_rose", layout.inputs.single { it.row == 0 && it.column == 0 }.cropId)
        assertEquals("red_mushroom", layout.inputs.single { it.row == 7 && it.column == 0 }.cropId)
        assertFalse(layout.inputs.any { it.row == 0 && it.column == 1 })
    }

    @Test
    fun `reject malformed SkyLayouts grid`() {
        assertThrows(IllegalArgumentException::class.java) {
            SkyLayoutsLayoutCodec.decode("https://skylayouts.io/layout?p=1#b1=2~a~WHEAT~0")
        }
    }

    @Test
    fun `decode Markdown escaped SkyLayouts link`() {
        val escapedUrl = FIXTURE.replace("&", "\\&").replace("~", "\\~").replace("_", "\\_")
        val escaped = "[$escapedUrl]($escapedUrl)"

        val layout = SkyLayoutsLayoutCodec.decode(escaped)

        assertEquals(72, layout.inputs.size)
        assertEquals("wild_rose", layout.inputs.single { it.row == 0 && it.column == 0 }.cropId)
        assertEquals("red_mushroom", layout.inputs.single { it.row == 7 && it.column == 0 }.cropId)
    }

    companion object {
        private const val FIXTURE = "https://skylayouts.io/layout?m=TIMESTALK&i=7&p=1&mode=edit#" +
            "b1=2~a~WILD_ROSE,NETHER_WART,CHORUS_FRUIT,PUMPKIN,SHELLFRUIT,STOPLIGHT_PETAL,WHEAT," +
            "ZOMBUD,RED_MUSHROOM~0.11.22.23.224545452464.12.12.12.147555454555.14.12.12.12.14." +
            "1245454524.14.12.12.12.148555454555.12.15.15.15.12.1442424244"
    }
}
