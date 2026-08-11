package at.hannibal2.skyhanni.test.inventory

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator.at
import at.hannibal2.skyhanni.config.features.inventory.customloadout.LoadoutKeybindConfig
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.lwjgl.glfw.GLFW

class LoadoutKeybindConfigTest {

    @Test
    fun `slot keybind groups have distinct defaults`() {
        val config = LoadoutKeybindConfig()

        assertEquals(
            listOf(
                GLFW.GLFW_KEY_1,
                GLFW.GLFW_KEY_2,
                GLFW.GLFW_KEY_3,
                GLFW.GLFW_KEY_4,
                GLFW.GLFW_KEY_5,
                GLFW.GLFW_KEY_6,
                GLFW.GLFW_KEY_7,
                GLFW.GLFW_KEY_8,
                GLFW.GLFW_KEY_9,
                GLFW.GLFW_KEY_UNKNOWN,
                GLFW.GLFW_KEY_UNKNOWN,
                GLFW.GLFW_KEY_UNKNOWN,
            ),
            config.slotKeybinds.asList(),
        )
        assertEquals(List(12) { GLFW.GLFW_KEY_UNKNOWN }, config.contestSlotKeybinds.asList())
    }

    @Test
    fun `flat slot keybinds migrate into accordion groups`() {
        val old = JsonObject()
        val oldKeybinds = old.at("inventory.customLoadout.keybinds".split("."), true) as JsonObject
        oldKeybinds.addProperty("slot1", GLFW.GLFW_KEY_Q)
        oldKeybinds.addProperty("contestSlot1", GLFW.GLFW_KEY_R)
        val event = ConfigUpdaterMigrator.ConfigFixEvent(
            old = old,
            new = JsonObject(),
            oldVersion = 142,
            movesPerformed = 0,
            dynamicPrefix = emptyMap(),
        )

        LoadoutKeybindConfig.migrateSlotKeybinds(event)

        val migrated = event.new.at("inventory.customLoadout.keybinds".split("."), false) as JsonObject
        assertEquals(GLFW.GLFW_KEY_Q, migrated["slotKeybinds"].asJsonObject["slot1"].asInt)
        assertEquals(GLFW.GLFW_KEY_R, migrated["contestSlotKeybinds"].asJsonObject["slot1"].asInt)
        assertFalse(oldKeybinds.has("slot1"))
        assertFalse(oldKeybinds.has("contestSlot1"))
    }
}
