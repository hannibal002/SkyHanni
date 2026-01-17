package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.addEnchantGlint
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatchesComponent
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object StereoHarmonyDiscReplacer {

    private val config get() = PestApi.config.stereoHarmony

    private val iconCache: MutableMap<String, ItemStack> = mutableMapOf()

    // TODO cache. load on inventory open only once, then read from a map slotId -> item stack
    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.replaceMenuIcons) return
        if (!PestApi.stereoInventory.isInside()) return
        if (event.slot !in 11..15 && event.slot !in 20..24 && event.slot !in 30..32) return

        val item = event.originalItem
        val internalName = item?.getInternalNameOrNull() ?: return
        val vinylType = VinylType.getByInternalNameOrNull(internalName) ?: return
        val cropType = PestType.getByVinylOrNull(vinylType)?.crop ?: return
        val lore = item.getLoreComponent()
        val isActiveVinyl = PestApi.stereoPlayingItemPattern.anyMatchesComponent(lore)
        val iconId = "stereo_harmony_replacer:${vinylType.name}-$isActiveVinyl"

        val replacementStack = iconCache.getOrPut(iconId) {
            cropType.getItemStackCopy(iconId).apply {
                if (isActiveVinyl) addEnchantGlint()
                setLore(lore)
                setCustomItemName(item.hoverName)
            }
        }

        event.replace(replacementStack)
    }
}
