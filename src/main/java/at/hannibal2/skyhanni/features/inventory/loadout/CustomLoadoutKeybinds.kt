package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CustomLoadoutKeybinds {

    private val config get() = LoadoutApi.config.keybinds
    private val keybinds
        get() = listOf(
            config.slot1,
            config.slot2,
            config.slot3,
            config.slot4,
            config.slot5,
            config.slot6,
            config.slot7,
            config.slot8,
            config.slot9,
            config.slot10,
            config.slot11,
            config.slot12,
        )
    private var lastClick = SimpleTimeMark.farPast()

    @HandleEvent
    fun onGuiKeyPress(event: GuiKeyPressEvent) {
        if (handlePress()) event.cancel()
    }

    @HandleEvent
    fun onGuiMouseInput(event: GuiMouseInputEvent) {
        if (handlePress()) event.cancel()
    }

    private fun handlePress(): Boolean {
        if (!isEnabled()) return false
        val slots = LoadoutApi.slots.filter { it.isInCurrentPage() }

        for ((index, key) in keybinds.withIndex()) {
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break
            val slot = slots.getOrNull(index) ?: continue

            LoadoutApi.clickSlot(slot)
            lastClick = SimpleTimeMark.now()
            return true
        }

        return false
    }

    fun allowMouseClick() = isEnabled() && keybinds.filter { it < 0 }.any { it.isKeyHeld() }
    fun allowKeyboardClick() = isEnabled() && keybinds.filter { it > 0 }.any { it.isKeyHeld() }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && LoadoutApi.inLoadouts() && config.slotKeybindsToggle
}
