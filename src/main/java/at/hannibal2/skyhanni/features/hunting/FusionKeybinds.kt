package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.ClickType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FusionKeybinds {

    private val config get() = SkyHanniMod.feature.hunting.fusionKeybinds

    private val patternGroup = RepoPattern.group("hunting.fusion.keybinds")

    // <editor-fold desc="Patterns">
    private val fusionBoxInventoryPattern by patternGroup.pattern(
        "inventory.fusion-box",
        "Fusion Box",
    )

    private val confirmFusionInventoryPattern by patternGroup.pattern(
        "inventory.confirm-fusion",
        "Confirm Fusion",
    )
    // </editor-fold>

    private val fusionBoxInventory = InventoryDetector { name ->
        fusionBoxInventoryPattern.matches(name)
    }
    private val confirmFusionInventory = InventoryDetector { name ->
        confirmFusionInventoryPattern.matches(name)
    }

    private var lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()

    // No island check because Kysha Abiphone contact exists
    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiKeyPress() {
        val repeatFusionKeybind = config.repeatFusionKeybind.get()
        val confirmFusionKeybind = config.confirmFusionKeybind.get()

        when {
            fusionBoxInventory.isInside() && repeatFusionKeybind.isKeyHeld() -> {
                if (confirmFusionKeybind.isKeyHeld()) return
                InventoryUtils.clickSlot(47, mouseButton = 2, mode = ClickType.MIDDLE)
            }

            confirmFusionInventory.isInside() && confirmFusionKeybind.isKeyHeld() -> {
                if (repeatFusionKeybind.isKeyHeld()) return
                InventoryUtils.clickSlot(33, mouseButton = 2, mode = ClickType.MIDDLE)
            }
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
