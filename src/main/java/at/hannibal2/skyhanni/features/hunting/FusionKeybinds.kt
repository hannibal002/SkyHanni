package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.ClickType
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FusionKeybinds {

    private val config get() = SkyHanniMod.feature.hunting.fusionKeybinds

    private var lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()

    // No island check because Kysha Abiphone contact exists
    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiKeyPress() {
        val repeatHeld = config.repeatFusionKeybind.get().isKeyHeld()
        val confirmHeld = config.confirmFusionKeybind.get().isKeyHeld()

        when {
            AttributeShardsData.fusionBoxInventory.isInside() && repeatHeld && !confirmHeld ->
                InventoryUtils.clickSlot(47, mouseButton = 2, mode = ClickType.MIDDLE)

            AttributeShardsData.confirmFusionInventory.isInside() && confirmHeld && !repeatHeld ->
                InventoryUtils.clickSlot(33, mouseButton = 2, mode = ClickType.MIDDLE)
        }
    }

    fun warnDuplicateKeybinds() {
        if (lastDuplicateKeybindsWarnTime.passedSince() < 1.seconds) return

        lastDuplicateKeybindsWarnTime = SimpleTimeMark.now()
        ChatUtils.chatAndOpenConfig(
            "§bRepeat Fusion Keybind §eand §bConfirm Fusion Keybind §ecannot be the same!",
            config::repeatFusionKeybind,
        )
    }

    @HandleEvent
    fun onConfigLoad() {
        val repeatFusionKeybind = config.repeatFusionKeybind.get()
        val confirmFusionKeybind = config.confirmFusionKeybind.get()

        config.repeatFusionKeybind.afterChange {
            if (this != GLFW.GLFW_KEY_UNKNOWN && this == confirmFusionKeybind) {
                config.repeatFusionKeybind.set(GLFW.GLFW_KEY_UNKNOWN)
                warnDuplicateKeybinds()
            }
        }

        config.confirmFusionKeybind.afterChange {
            if (this != GLFW.GLFW_KEY_UNKNOWN && this == repeatFusionKeybind) {
                config.confirmFusionKeybind.set(GLFW.GLFW_KEY_UNKNOWN)
                warnDuplicateKeybinds()
            }
        }
    }
}
