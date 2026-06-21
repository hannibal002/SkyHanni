package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCropType
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import at.hannibal2.skyhanni.utils.SafeItemStack

@SkyHanniModule
object ToolkitCropReplacer {

    private val config get() = GardenApi.config.farmingToolkit

    private val iconCache: MutableMap<Int, SafeItemStack> = mutableMapOf()

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.replaceMenuIcons) return
        if (!GardenApi.toolkitInventory.isInside()) return
        if (event.slot !in 10..16 && event.slot !in 20..24) return

        val item = event.originalItem
        val cropType = item.getCropType() ?: return
        val iconId = "toolkit_crop_replacer:${cropType.name}"

        val replacementStack = iconCache.getOrPut(event.slot) {
            cropType.getItemStackCopy(iconId).apply {
                setLore(item.getLoreComponent())
                setCustomItemName(item.hoverName)
            }
        }

        event.replace(replacementStack.copy())
    }
}
