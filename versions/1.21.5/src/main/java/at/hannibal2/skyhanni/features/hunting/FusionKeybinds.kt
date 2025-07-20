package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FusionKeybinds {

    val config get() = SkyHanniMod.feature.hunting.fusionKeybinds

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
