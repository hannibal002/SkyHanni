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
import java.util.*
import kotlin.math.roundToInt

class FarmingFortuneDisplay {
    private val config get() = SkyHanniMod.feature.garden

    private val tabFortunePattern = " Farming Fortune: §r§6☘(\\d+)".toRegex()
    private val tooltipFortunePattern = "^§5§o§7Farming Fortune: §a\\+(\\d+)(.*)$".toRegex()

    private var display = listOf<Any>()
    private var currentCrop: CropType? = null

    private var tabFortune: Double = 0.0
    private var toolFortune: Double = 0.0
    private val upgradeFortune: Double?
        get() = currentCrop?.getUpgradeLevel()?.let { it * 5.0 }

    @SubscribeEvent
    fun onCropUpgradeUpdate(event: CropUpgradeUpdateEvent) {
        drawDisplay()
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        tabFortune = event.tabList.firstNotNullOfOrNull {
            tabFortunePattern.matchEntire(it)?.groups?.get(1)?.value?.toDoubleOrNull()
        } ?: tabFortune
        drawDisplay()
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onInventoryUpdate(event: OwnInventorItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        if (GardenAPI.getCropTypeFromItem(event.itemStack) == null) return
        updateToolFortune(event.itemStack)
    }

    @SubscribeEvent
    fun onBlockBreak(event: BlockClickEvent) {
        if (!GardenAPI.inGarden()) return
        val cropBroken = CropType.getByBlock(event.getBlockState) ?: return
        if (cropBroken != currentCrop) {
            currentCrop = cropBroken
            updateToolFortune(event.itemInHand)
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val heldTool = event.toolItem
        currentCrop = event.crop ?: currentCrop
        updateToolFortune(heldTool)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!GardenAPI.inGarden() || !config.farmingFortuneDisplay) return
        config.farmingFortunePos.renderSingleLineWithItems(display, posLabel = "Farming Fortune")
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val crop = GardenAPI.getCropTypeFromItem(event.itemStack) ?: return
        val toolFortune = getToolFortune(event.itemStack)
        val counterFortune = getCounterFortune(event.itemStack)
        val turboCropFortune = getTurboCropFortune(event.itemStack, crop)
        val dedicationFortune = getDedicationFortune(event.itemStack, crop)
        val cropFortune = (toolFortune + counterFortune + turboCropFortune + dedicationFortune).roundToInt()
        val iterator = event.toolTip.listIterator()
        for (line in iterator) {
            val match = tooltipFortunePattern.matchEntire(line)?.groups ?: continue
            val displayedFortune = match[1]!!.value.toInt()
            val theRest = match[2]!!.value
            iterator.set("§7Farming Fortune: §a+${displayedFortune + cropFortune}$theRest §6[+$cropFortune]")
        }
    }

    private fun drawDisplay() {
        val displayCrop = currentCrop ?: return
        val updatedDisplay = mutableListOf<Any>()
        GardenAPI.addGardenCropToList(displayCrop, updatedDisplay)
        updatedDisplay.add(upgradeFortune?.let {
            val totalFortune = it + tabFortune + toolFortune
            "§6Farming Fortune§7: §e${LorenzUtils.formatDouble(totalFortune, 0)}"
        } ?: "§cOpen §e/cropupgrades§c to use!")
        display = updatedDisplay
    }

    private fun updateToolFortune(tool: ItemStack?) {
        val cropMatchesTool = currentCrop == GardenAPI.getCropTypeFromItem(tool)
        val toolCounterFortune = if (cropMatchesTool) {
            getToolFortune(tool) + getCounterFortune(tool)
        } else 0.0
        toolFortune = toolCounterFortune +
                getTurboCropFortune(tool, currentCrop) +
                getDedicationFortune(tool, currentCrop)
        drawDisplay()
    }

    companion object {
        private val counterPattern = "§7You have §6\\+([\\d]{1,3})☘ Farming Fortune".toRegex()

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

        private fun getTurboCropFortune(tool: ItemStack?, cropType: CropType?): Double {
            val crop = cropType ?: return 0.0
            return tool?.getEnchantments()?.get(crop.getTurboCrop())?.let { it * 5.0 } ?: 0.0
        }

        private fun getCounterFortune(tool: ItemStack?): Double {
            val lore = tool?.getLore() ?: return 0.0
            return lore.sumOf {
                counterPattern.matchEntire(it)?.groups?.get(1)?.value?.toDoubleOrNull() ?: 0.0
            }
        }

        private fun getDedicationFortune(tool: ItemStack?, cropType: CropType?): Double {
            val dedicationLevel = tool?.getEnchantments()?.get("dedication") ?: 0
            val dedicationMultiplier = listOf(0.0, 0.5, 0.75, 1.0, 2.0)[dedicationLevel]
            val cropMilestone = GardenCropMilestones.getTierForCrops(
                cropType?.getCounter() ?: 0
            )
            return dedicationMultiplier * cropMilestone
        }
    }
}