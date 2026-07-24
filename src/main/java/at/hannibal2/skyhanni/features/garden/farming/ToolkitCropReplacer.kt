package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCropType
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData

@SkyHanniModule
object ToolkitCropReplacer {

    private val config get() = GardenApi.config.farmingToolkit

    private val iconCache: MutableMap<Int, SafeItemStack> = mutableMapOf()
    private val storedTools: MutableMap<Int, CropType> = mutableMapOf()

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.replaceMenuIcons) return
        if (!GardenApi.toolkitInventory.isInside()) return
        if (!event.hasItem) return

        val slot = event.slot
        if (slot !in 10..16 && slot !in 20..24) return

        val item = event.originalItem
        val cropType = item.getCropType()
        if (cropType == null) {
            storedTools.remove(slot)
            iconCache.remove(slot)
            return
        }

        // For swapping sunflower/moonflower icon
        if (storedTools[slot] != cropType) {
            iconCache.remove(slot)
        }
        storedTools[slot] = cropType

        val replacementStack = iconCache.getOrPut(slot) {
            createIcon(item, cropType)
        }

        event.replace(replacementStack.copy())
    }

    private fun createIcon(item: SafeItemStack, cropType: CropType): SafeItemStack {
        val iconId = "toolkit_crop_replacer:${cropType.name}"

        return cropType.getItemStackCopy(iconId).apply {
            setLore(item.getLoreComponent())
            setCustomItemName(item.hoverName)
            set(DataComponents.CUSTOM_DATA, item.getExtraAttributes()?.let { CustomData.of(it) })
        }
    }

    @HandleEvent
    fun onProfileJoin() {
        storedTools.clear()
        iconCache.clear()
    }
}
