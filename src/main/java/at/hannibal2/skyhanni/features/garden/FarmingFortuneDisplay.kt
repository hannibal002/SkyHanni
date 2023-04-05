package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropUpgrades.Companion.getUpgradeLevel
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getTurboCrop
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FarmingFortuneDisplay {
    private val config get() = SkyHanniMod.feature.garden

    private val tabFortunePattern = " Farming Fortune: §r§6☘(\\d+)".toRegex()
    private val counterPattern = "§7You have §6\\+([\\d]{1,3})☘ Farming Fortune".toRegex()

    private var display = mutableListOf<Any>()
    private var currentCrop: CropType? = null

    private var tabFortune: Double = 0.0
    private var toolFortune: Double = 0.0
    private var counterFortune: Double = 0.0
    private var turboCropFortune: Double = 0.0
    private var dedicationFortune: Double = 0.0
    private var upgradeFortune: Double = 0.0

    private val totalFortune: Double
        get() = tabFortune +
                toolFortune +
                counterFortune +
                turboCropFortune +
                dedicationFortune +
                upgradeFortune

    @SubscribeEvent
    fun onBlockBreak(event: BlockClickEvent) {
        if (!GardenAPI.inGarden()) return
        val cropBroken = CropType.getByBlock(event.getBlockState) ?: return
        if (cropBroken != currentCrop) {
            currentCrop = cropBroken
            updateCropSpecificFortune(event.itemInHand)
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val heldTool = event.toolItem
        currentCrop = event.crop ?: currentCrop
        updateCropSpecificFortune(heldTool)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onInventoryUpdate(event: OwnInventorItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        if (GardenAPI.getCropTypeFromItem(event.itemStack) == null) return
        updateCropSpecificFortune(event.itemStack)
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        tabFortune = event.tabList.firstNotNullOfOrNull {
            tabFortunePattern.matchEntire(it)?.groups?.get(1)?.value?.toDoubleOrNull()
        } ?: tabFortune
        drawDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!GardenAPI.inGarden() || !config.farmingFortuneDisplay) return
        config.farmingFortunePos.renderSingleLineWithItems(display, posLabel = "Farming Fortune")
    }

    private fun drawDisplay() {
        val displayCrop = currentCrop ?: return
        display.clear()
        GardenAPI.addGardenCropToList(displayCrop, display)
        display.add("§6Farming Fortune§7: §e${LorenzUtils.formatDouble(totalFortune, 0)}")
    }

    private fun updateCropSpecificFortune(tool: ItemStack?) {
        val cropMatchesTool = currentCrop == GardenAPI.getCropTypeFromItem(tool)
        toolFortune = if (cropMatchesTool) getToolFortune(tool) else 0.0
        counterFortune = if (cropMatchesTool) getCounterFortune(tool) else 0.0

        turboCropFortune = getTurboCropFortune(tool)
        dedicationFortune = getDedicationFortune(tool)
        upgradeFortune = (currentCrop?.getUpgradeLevel() ?: 0) * 5.0
    }

    private fun getTurboCropFortune(tool: ItemStack?): Double {
        val crop = currentCrop ?: return 0.0
        return tool?.getEnchantments()?.get(crop.getTurboCrop())?.let { it * 5.0 } ?: 0.0
    }

    private fun getToolFortune(tool: ItemStack?): Double {
        val internalName = tool?.getInternalName() ?: return 0.0
        return if (internalName.startsWith("THEORETICAL_HOE")) {
            listOf(10.0, 25.0, 50.0)[internalName.last().digitToInt() - 1]
        } else when (internalName) {
            "FUNGI_CUTTER" -> 30.0
            "COCO_CHOPPER" -> 20.0
            else -> 0.0
        }
    }

    private fun getDedicationFortune(tool: ItemStack?): Double {
        val dedicationLevel = tool?.getEnchantments()?.get("dedication") ?: 0
        val dedicationMultiplier = listOf(0.0, 0.5, 0.75, 1.0, 2.0)[dedicationLevel]
        val cropMilestone = GardenCropMilestones.getTierForCrops(
            currentCrop?.getCounter() ?: 0
        )
        return dedicationMultiplier * cropMilestone
    }

    private fun getCounterFortune(tool: ItemStack?): Double {
        val lore = tool?.getLore() ?: return 0.0
        return lore.sumOf {
            counterPattern.matchEntire(it)?.groups?.get(1)?.value?.toDoubleOrNull() ?: 0.0
        }
    }
}