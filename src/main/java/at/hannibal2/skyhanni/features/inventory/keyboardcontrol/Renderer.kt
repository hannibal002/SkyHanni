package at.hannibal2.skyhanni.features.inventory.keyboardcontrol

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import org.lwjgl.input.Keyboard

@SkyHanniModule
object Renderer {
    private val config get() = SkyHanniMod.feature.inventory.keyboardControl

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!isEnabled()) return
        val slotNumber = event.slot.slotNumber
        val keyActions = InventoryKeybindSystem.handler.context?.slotToKeybinds?.get(slotNumber) ?: return

        val keybindText = keyActions
            .filter { it.key != Keyboard.KEY_NONE }
            .joinToString(separator = "/") { formatKeybind(it) }

        if (keybindText.isNotEmpty()) {
            event.stackTip = "§7[§b$keybindText§7]"
        }
    }

    private fun formatKeybind(action: SlotActionEntry): String {
        val modifiers = action.modifiers.joinToString(" + ") { KeyboardManager.getKeyName(it) }
        val mainKey = KeyboardManager.getKeyName(action.key)
        // TODO compact key & modifiers
        return if (modifiers.isNotEmpty()) {
            "$modifiers + $mainKey"
        } else {
            mainKey
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.renderEnabled && (config.keybindsEnabled || config.selectorEnabled)
}
