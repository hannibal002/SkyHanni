package at.hannibal2.hanni.features.inventory.wardrobe

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiKeyPressEvent
import at.hannibal2.hanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.hanni.features.inventory.wardrobe.CustomWardrobe.clickSlot
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object CustomWardrobeKeybinds {

    private val config get() = HanniMod.feature.inventory.customWardrobe
    private val keybinds
        get() = listOf(
            config.keybinds.slot1,
            config.keybinds.slot2,
            config.keybinds.slot3,
            config.keybinds.slot4,
            config.keybinds.slot5,
            config.keybinds.slot6,
            config.keybinds.slot7,
            config.keybinds.slot8,
            config.keybinds.slot9,
        )
    var lastClick = SimpleTimeMark.farPast()

    @HandleEvent
    fun onGui(event: GuiKeyPressEvent) {
        if (handlePress()) event.cancel()
    }

    @HandleEvent
    fun onMouseInput(event: GuiMouseInputEvent) {
        if (handlePress()) event.cancel()
    }

    private fun handlePress(): Boolean {
        if (!isEnabled()) return false
        val slots = WardrobeApi.slots.filter { it.isInCurrentPage() }
            .filterNot { config.onlyFavorites && !it.favorite }
            .filterNot { config.hideEmptySlots && it.armor.all { piece -> piece == null } }

        for ((index, key) in keybinds.withIndex()) {
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break
            val slot = slots.getOrNull(index) ?: continue

            slot.clickSlot()
            lastClick = SimpleTimeMark.now()
            return true
        }

        return false
    }

    fun allowMouseClick() = isEnabled() && keybinds.filter { it < 0 }.any { it.isKeyHeld() }
    fun allowKeyboardClick() = isEnabled() && keybinds.filter { it > 0 }.any { it.isKeyHeld() }

    private fun isEnabled() =
        SkyBlockUtils.inSkyBlock && WardrobeApi.inCustomWardrobe && config.keybinds.slotKeybindsToggle && config.enabled
}
