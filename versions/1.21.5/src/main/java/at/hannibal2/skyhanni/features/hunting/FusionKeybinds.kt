package at.hannibal2.hanni.features.hunting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.SimpleTimeMark
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.seconds

@HanniModule
object FusionKeybinds {

    val config get() = HanniMod.feature.hunting.fusionKeybinds

    private var lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()
    private var hasDuplicateKeybinds = false

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onKeybind(event: GuiKeyPressEvent) {
        if (hasDuplicateKeybinds) return
        when (InventoryUtils.openInventoryName()) {
            "Fusion Box" -> {
                if (!config.repeatFusionKeybind.isKeyHeld() || config.confirmFusionKeybind.isKeyHeld()) return
                InventoryUtils.clickSlot(47, mouseButton = 2, mode = 3)
            }

            "Confirm Fusion" -> {
                if (!config.confirmFusionKeybind.isKeyHeld() || config.repeatFusionKeybind.isKeyHeld()) return
                InventoryUtils.clickSlot(33, mouseButton = 2, mode = 3)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSecondPassed(event: SecondPassedEvent) {
        hasDuplicateKeybinds = config.repeatFusionKeybind != GLFW.GLFW_KEY_UNKNOWN &&
            config.confirmFusionKeybind != GLFW.GLFW_KEY_UNKNOWN &&
            config.repeatFusionKeybind == config.confirmFusionKeybind

        if (!hasDuplicateKeybinds || lastDuplicateKeybindsWarnTime.passedSince() < 30.seconds) return
        ChatUtils.chatAndOpenConfig(
            "Repeat Fusion and Confirm Fusion keybinds cannot be the same!",
            config::repeatFusionKeybind,
        )
        lastDuplicateKeybindsWarnTime = SimpleTimeMark.now()
    }
}
