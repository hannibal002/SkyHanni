package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.clickSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CustomWardrobeKeybinds {

    private val config get() = SkyHanniMod.feature.inventory.customWardrobe
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

    @SubscribeEvent
    fun onGui(event: GuiKeyPressEvent) {
        if (!isEnabled()) return
        val slots = WardrobeAPI.slots.filter { it.isInCurrentPage() }

        for ((key, index) in keybinds.withIndex().map { it.value to it.index }) {
            if (!key.isKeyClicked()) continue
            if (lastClick.passedSince() < 200.milliseconds) break
            val slot = slots.getOrNull(index) ?: continue

            event.cancel()

            slot.clickSlot()
            lastClick = SimpleTimeMark.now()
            break
        }
    }

    fun allowKeyboardClick() = isEnabled() && keybinds.any { it.isKeyClicked() }

    private fun isEnabled() = LorenzUtils.inSkyBlock && WardrobeAPI.inCustomWardrobe && config.keybinds.slotKeybindsToggle && config.enabled

}
