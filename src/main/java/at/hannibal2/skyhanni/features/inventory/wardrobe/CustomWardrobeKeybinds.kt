package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.clickSlot
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.milliseconds

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
    private var lastClick = SimpleTimeMark.farPast()

    internal fun handlePress(): Boolean {
        if (!isEnabled()) return false
        val slots = ArmorWardrobeApi.slots.filter { it.isInCurrentPage() }
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
        SkyBlockUtils.inSkyBlock && CustomWardrobe.inCustomWardrobe && config.keybinds.slotKeybindsToggle && config.enabled
}
