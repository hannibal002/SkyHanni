package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.config.ConfigGuiManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigCategoryNamesTest {

    @Test
    fun `category names are extracted from the config categories`() {
        assertEquals(
            listOf(
                "About",
                "GUI",
                "Garden",
                "Crimson Isle",
                "The Rift",
                "Fishing",
                "Mining",
                "Foraging",
                "Hunting",
                "Combat",
                "Slayer",
                "Dungeon",
                "Inventory",
                "Events",
                "Skill Progress",
                "Chat",
                "Misc",
                "Dev",
            ),
            ConfigGuiManager.categoryNames,
        )
    }
}
