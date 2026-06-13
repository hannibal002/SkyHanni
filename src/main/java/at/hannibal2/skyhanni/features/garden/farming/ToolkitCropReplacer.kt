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

    private var fullyLoaded = false

    @HandleEvent
    fun onInventoryFullyOpened() {
        if (!GardenApi.toolkitInventory.isInside()) return
        fullyLoaded = true
    }

    @HandleEvent
    fun onInventoryClose() {
        fullyLoaded = false
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.replaceMenuIcons) return
        if (!GardenApi.toolkitInventory.isInside()) return
        if (event.slot !in 10..16 && event.slot !in 20..24) return

        val item = event.originalItem
        if (item.isEmpty) {
            if (!fullyLoaded) return
            iconCache.remove(event.slot)
        }
        val cropType = item.getCropType()
        if (cropType == null) {
            storedTools.remove(event.slot)
            iconCache.remove(event.slot)
            return
        }

        // For swapping sunflower/moonflower icon
        if (storedTools[event.slot] != cropType) {
            iconCache.remove(event.slot)
        }
        storedTools[event.slot] = cropType

        val iconId = "toolkit_crop_replacer:${cropType.name}"

        val replacementStack = iconCache.getOrPut(event.slot) {
            val stack = cropType.getItemStackCopy(iconId).apply {
                setLore(item.getLoreComponent())
                setCustomItemName(item.hoverName)
            }
            stack.set(DataComponents.CUSTOM_DATA, item.getExtraAttributes()?.let { CustomData.of(it) })
            return@getOrPut stack
        }

        event.replace(replacementStack.copy())
    }

    @HandleEvent
    fun onProfileJoin() {
        storedTools.clear()
        iconCache.clear()
    }
}
