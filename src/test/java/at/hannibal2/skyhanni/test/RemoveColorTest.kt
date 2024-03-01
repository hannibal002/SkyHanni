package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RemoveColorTest {
    @Test
    fun testEdging() {
        Assertions.assertEquals("", "§".removeColor())
        Assertions.assertEquals("a", "a§".removeColor())
        Assertions.assertEquals("b", "§ab§".removeColor())
    }

    @Test
    fun `testDouble§`() {
        Assertions.assertEquals("1", "§§1".removeColor())
        Assertions.assertEquals("1", "§§1".removeColor(true))
        Assertions.assertEquals("k", "§§k".removeColor(true))
    }

    @Test
    fun testKeepNonColor() {
        Assertions.assertEquals("§k§l§m§n§o§r", "§k§l§m§f§n§o§r".removeColor(true))
    }

    @Test
    fun testPlainString() {
        Assertions.assertEquals("bcdefgp", "bcdefgp")
        Assertions.assertEquals("", "")
    }

    @Test
    fun testSomeNormalTestCases() {
        Assertions.assertEquals(
            "You are not currently in a party.",
            "§r§cYou are not currently in a party.§r".removeColor()
        )
        Assertions.assertEquals(
            "Ancient Necron's Chestplate ✪✪✪✪",
            "§dAncient Necron's Chestplate §6✪§6✪§6✪§6✪".removeColor()
        )
    }

}
