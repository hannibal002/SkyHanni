package at.hannibal2.hanni.features.garden.pests.stereo

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.hanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.hanni.features.garden.pests.PestApi
import at.hannibal2.hanni.features.garden.pests.PestType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.addEnchantGlint
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.ItemUtils.setLore
import at.hannibal2.hanni.utils.RegexUtils.anyMatches
import at.hannibal2.hanni.utils.compat.setCustomItemName
import net.minecraft.item.ItemStack

@HanniModule
object StereoHarmonyDiscReplacer {

    private val config get() = PestApi.config.stereoHarmony

    private val iconCache: MutableMap<String, ItemStack> = mutableMapOf()

    // TODO cache. load on inventory open only once, then read from a map slotId -> item stack
    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.replaceMenuIcons) return
        if (!PestApi.stereoInventory.isInside()) return
        if (event.slot !in 11..15 && event.slot !in 20..24) return

        val item = event.originalItem
        val internalName = item?.getInternalNameOrNull() ?: return
        val vinylType = VinylType.getByInternalNameOrNull(internalName) ?: return
        val cropType = PestType.getByVinylOrNull(vinylType)?.crop ?: return
        val lore = item.getLore()
        val isActiveVinyl = PestApi.stereoPlayingItemPattern.anyMatches(lore)
        val iconId = "stereo_harmony_replacer:${vinylType.name}-$isActiveVinyl"

        val replacementStack = iconCache.getOrPut(iconId) {
            cropType.getItemStackCopy(iconId).apply {
                if (isActiveVinyl) addEnchantGlint()
                setLore(lore)
                setCustomItemName(item.displayName)
            }
        }

        event.replace(replacementStack)
    }
}
