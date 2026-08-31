package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.SkyHanniConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigOptionIndexTest {

    private val optionPaths = ConfigGuiManager.collectOptionPaths(SkyHanniConfig())

    @Test
    fun `all config options are collected`() {
        assertTrue(optionPaths.isNotEmpty())
        assertTrue(optionPaths.size > 500)
    }

    @Test
    fun `option paths contain known examples`() {
        assertTrue("garden.noBreakItems" in optionPaths)
        assertTrue("garden.cropMilestones.progress" in optionPaths)
        assertTrue("chat.filterType.empty" in optionPaths)
    }

    @Test
    fun `option paths are well formed`() {
        assertFalse(optionPaths.keys.any { it.startsWith("config.") })
        assertFalse(optionPaths.keys.any { it.contains(' ') })
    }
}
