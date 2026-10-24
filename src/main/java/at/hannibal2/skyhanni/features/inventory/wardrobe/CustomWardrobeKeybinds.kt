package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.milliseconds

object CustomWardrobeKeybinds {

    private val config get() = SkyHanniMod.feature.inventory.customLoadout.wardrobe
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

    internal fun handlePress(wardrobe: AbstractCustomWardrobe) {
        if (!isEnabled(wardrobe)) return
        val slots = wardrobe.api.slots.filter { it.isInCurrentPage() }
            .filterNot { wardrobe.onlyFavorites && !it.favorite }
            .filterNot { config.hideEmptySlots && it.armor.all { piece -> piece == null } }

        for ((index, key) in keybinds.withIndex()) {
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break
            val slot = slots.getOrNull(index) ?: continue

            with(wardrobe) { slot.clickSlot() }
            lastClick = SimpleTimeMark.now()
        }
    }

    private fun isEnabled(wardrobe: AbstractCustomWardrobe) =
        SkyBlockUtils.inSkyBlock && wardrobe.inCustomMenu && config.keybinds.slotKeybindsToggle
}
