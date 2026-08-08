package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.config.ConfigGuiManager
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigSearchMatcherTest {

    private fun createOption(fulfillsSearch: (String) -> Boolean): ProcessedOption {
        val editor = mockk<GuiOptionEditor>()
        every { editor.fulfillsSearch(any()) } answers { fulfillsSearch(arg<String>(0)) }
        return mockk {
            every { getEditor() } returns editor
        }
    }

    @Test
    fun `matches when the path contains the search`() {
        val option = createOption { false }
        assertTrue(ConfigGuiManager.configOptionMatchesSearch("noBreak", "garden.noBreakItems", option))
    }

    @Test
    fun `matches via MoulConfig search including search tags`() {
        val option = createOption { it == "milestone" }
        assertTrue(ConfigGuiManager.configOptionMatchesSearch("milestone", "garden.progress", option))
        assertFalse(ConfigGuiManager.configOptionMatchesSearch("other", "garden.progress", option))
    }

    @Test
    fun `search is case insensitive`() {
        val option = createOption { false }
        assertTrue(ConfigGuiManager.configOptionMatchesSearch("NOBREAK", "garden.noBreakItems", option))
    }

    @Test
    fun `empty search matches everything`() {
        val option = createOption { false }
        assertTrue(ConfigGuiManager.configOptionMatchesSearch("", "garden.progress", option))
        assertTrue(ConfigGuiManager.configOptionMatchesSearch("   ", "garden.progress", option))
    }

    @Test
    fun `all plus separated terms must match`() {
        val option = createOption { it == "a" }
        assertTrue(ConfigGuiManager.configOptionMatchesSearch("a+b", "garden.a.b", option))
        assertFalse(ConfigGuiManager.configOptionMatchesSearch("a+x", "garden.a.b", option))
    }

    @Test
    fun `category names match with the same semantics`() {
        assertTrue(ConfigGuiManager.categoryMatchesSearch("Crimson Isle", "crimson"))
        assertTrue(ConfigGuiManager.categoryMatchesSearch("The Rift", "rift"))
        assertFalse(ConfigGuiManager.categoryMatchesSearch("About", "garden"))
        assertTrue(ConfigGuiManager.categoryMatchesSearch("About", ""))
    }
}
