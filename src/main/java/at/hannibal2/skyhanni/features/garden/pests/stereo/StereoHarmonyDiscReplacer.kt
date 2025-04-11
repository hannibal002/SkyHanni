package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import net.minecraft.item.ItemStack

@SkyHanniModule
object StereoHarmonyDiscReplacer {

    private val config get() = PestApi.config.stereoHarmony
    private val inventoryPattern by PestApi.patternGroup.pattern(
        "stereo.inventory",
        "Stereo Harmony"
    )
    private val iconCache: MutableMap<String, ItemStack> = mutableMapOf()

    // TODO cache. load on invenotry open only once, then read from a map slotId -> item stack
    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.replaceMenuIcons) return
        if (!inventoryPattern.matches(event.inventory.name)) return
        if (event.slot !in 11..15 && event.slot !in 20..24) return

        val internalName = event.originalItem.getInternalNameOrNull() ?: return
        val vinylType = VinylType.getByInternalNameOrNull(internalName) ?: return
        val cropType = PestType.getByVinylOrNull(vinylType)?.crop ?: return
        val isActiveVinyl = StereoHarmonyDisplay.activeVinyl == vinylType
        val iconId = "stereo_harmony_replacer:${vinylType.name}-$isActiveVinyl"

        val replacementStack = iconCache.getOrPut(iconId) {
            cropType.getItemStackCopy(iconId).apply {
                if (isActiveVinyl) addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 0)
                setLore(event.originalItem.getLore())
                setStackDisplayName(event.originalItem.displayName)
            }
        }

        event.replace(replacementStack)
    }
}
