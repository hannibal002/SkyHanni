package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.GardenCropUpgrades.Companion.getUpgradeLevel
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getTurboCrop
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeCounter
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor
import kotlin.math.log10

class FarmingFortuneDisplay {
    private val tabFortunePattern = " Farming Fortune: §r§6☘(\\d+)".toRegex()

    private var display = emptyList<List<Any>>()
    private var accessoryProgressDisplay = ""

    private var lastToolSwitch: Long = 0

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        accessoryProgressDisplay = ""
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        tabFortune = event.tabList.firstNotNullOfOrNull {
            tabFortunePattern.matchEntire(it)?.groups?.get(1)?.value?.toDoubleOrNull()
        } ?: tabFortune
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onInventoryUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.itemStack.getCropType() == null) return
        updateToolFortune(event.itemStack)
    }

    @SubscribeEvent
    fun onBlockBreak(event: CropClickEvent) {
        val cropBroken = event.crop
        if (cropBroken != currentCrop) {
            updateToolFortune(event.itemInHand)
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastToolSwitch = System.currentTimeMillis()
        updateToolFortune(event.toolItem)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.hideExtraGuis()) return
        config.pos.renderStringsAndItems(display, posLabel = "True Farming Fortune")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!CropAccessoryData.isLoadingAccessories) return
        SkyHanniMod.feature.misc.inventoryLoadPos.renderString(
            accessoryProgressDisplay,
            posLabel = "Load Accessory Bags"
        )
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return
        val displayCrop = currentCrop ?: return

        val updatedDisplay = mutableListOf<List<Any>>()
        updatedDisplay.add(mutableListOf<Any>().also {
            it.addCropIcon(displayCrop)
            val recentlySwitchedTool = System.currentTimeMillis() < lastToolSwitch + 1000
            it.add(
                "§6Farming Fortune§7: §e" + if (!recentlySwitchedTool) {
                    LorenzUtils.formatDouble(getCurrentFarmingFortune(), 0)
                } else "?"
            )
            if (GardenAPI.toolInHand != null) {
                latestFF?.put(displayCrop, getCurrentFarmingFortune(true))
            }
        })

        if (upgradeFortune == null) {
            updatedDisplay.addAsSingletonList("§cOpen §e/cropupgrades§c for more exact data!")
        }
        if (accessoryFortune == null) {
            updatedDisplay.addAsSingletonList("§cOpen Accessory Bag for more exact data!")
            if (CropAccessoryData.isLoadingAccessories) {
                accessoryProgressDisplay =
                    "§e${CropAccessoryData.pagesLoaded}/${CropAccessoryData.accessoryBagPageCount} pages viewed"
            }
        } else {
            accessoryProgressDisplay = ""
        }

        display = updatedDisplay
    }

    private fun updateToolFortune(tool: ItemStack?) {
        val cropMatchesTool = currentCrop == tool?.getCropType()
        val toolCounterFortune = if (cropMatchesTool) {
            getToolFortune(tool) + getCounterFortune(tool) + getCollectionFortune(tool)
        } else 0.0
        toolFortune =
            toolCounterFortune + getTurboCropFortune(tool, currentCrop) + getDedicationFortune(tool, currentCrop)
    }

    private fun isEnabled(): Boolean = GardenAPI.inGarden() && config.display


    companion object {
        private val config get() = SkyHanniMod.feature.garden.farmingFortunes
        private val latestFF: MutableMap<CropType, Double>? get() = GardenAPI.config?.latestTrueFarmingFortune

        private val currentCrop get() = GardenAPI.getCurrentlyFarmedCrop()

        private var tabFortune: Double = 0.0
        private var toolFortune: Double = 0.0
        private val baseFortune: Double get() = if (config.dropMultiplier) 100.0 else 0.0
        private val upgradeFortune: Double? get() = currentCrop?.getUpgradeLevel()?.let { it * 5.0 }
        private val accessoryFortune: Double?
            get() = currentCrop?.let {
                CropAccessoryData.cropAccessory?.getFortune(it)
            }

        private val collectionPattern = "§7You have §6\\+([\\d]{1,3})☘ Farming Fortune".toRegex()
        private val tooltipFortunePattern =
            "^§7Farming Fortune: §a\\+([\\d.]+)(?: §2\\(\\+\\d\\))?(?: §9\\(\\+(\\d+)\\))?$".toRegex()
        private val armorAbilityPattern = "Tiered Bonus: .* [(](?<pieces>.*)/4[)]".toPattern()

        var displayedFortune = 0.0
        var reforgeFortune = 0.0
        var itemBaseFortune = 0.0
        var greenThumbFortune = 0.0

        fun getToolFortune(tool: ItemStack?): Double = getToolFortune(tool?.getInternalName())
        fun getToolFortune(internalName: NEUInternalName?): Double {
            if (internalName == null) return 0.0
            if (internalName.equals("THEORETICAL_HOE")) {
                return 0.0
            }
            return if (internalName.startsWith("THEORETICAL_HOE")) {
                listOf(10.0, 25.0, 50.0)[internalName.asString().last().digitToInt() - 1]
            } else when (internalName.asString()) {
                "FUNGI_CUTTER" -> 30.0
                "COCO_CHOPPER" -> 20.0
                else -> 0.0
            }
        }

        fun getTurboCropFortune(tool: ItemStack?, cropType: CropType?): Double {
            val crop = cropType ?: return 0.0
            return tool?.getEnchantments()?.get(crop.getTurboCrop())?.let { it * 5.0 } ?: 0.0
        }

        fun getCollectionFortune(tool: ItemStack?): Double {
            val lore = tool?.getLore() ?: return 0.0
            var hasCollectionAbility = false
            return lore.firstNotNullOfOrNull {
                if (hasCollectionAbility || it == "§6Collection Analysis") {
                    hasCollectionAbility = true
                    collectionPattern.matchEntire(it)?.groups?.get(1)?.value?.toDoubleOrNull()
                } else null
            } ?: 0.0
        }

        fun getCounterFortune(tool: ItemStack?): Double {
            val counter = tool?.getHoeCounter() ?: return 0.0
            val digits = floor(log10(counter.toDouble()))
            return (16 * digits - 48).coerceAtLeast(0.0)
        }

        fun getDedicationFortune(tool: ItemStack?, cropType: CropType?): Double {
            if (cropType == null) return 0.0
            val dedicationLevel = tool?.getEnchantments()?.get("dedication") ?: 0
            val dedicationMultiplier = listOf(0.0, 0.5, 0.75, 1.0, 2.0)[dedicationLevel]
            val cropMilestone = GardenCropMilestones.getTierForCropCount(
                cropType.getCounter(), cropType
            )
            return dedicationMultiplier * cropMilestone
        }

        fun getSunderFortune(tool: ItemStack?) = (tool?.getEnchantments()?.get("sunder") ?: 0) * 12.5
        fun getHarvestingFortune(tool: ItemStack?) = (tool?.getEnchantments()?.get("harvesting") ?: 0) * 12.5
        fun getCultivatingFortune(tool: ItemStack?) = (tool?.getEnchantments()?.get("cultivating") ?: 0) * 2.0

        fun getAbilityFortune(item: ItemStack?) = item?.let {
            getAbilityFortune(it.getInternalName(), it.getLore())
        } ?: 0.0

        fun getAbilityFortune(internalName: NEUInternalName, lore: List<String>): Double {
            val lotusAbilityPattern = "§7Piece Bonus: §6+(?<bonus>.*)☘".toPattern()
            // todo make it work on Melon and Cropie armor
            val armorAbilityFortune = "§7.*§7Grants §6(?<bonus>.*)☘.*".toPattern()
            var pieces = 0

            lore.forEach { line ->
                if (internalName.contains("LOTUS")) {
                    lotusAbilityPattern.matchMatcher(line) {
                        return group("bonus").toDouble()
                    }
                }
                armorAbilityPattern.matchMatcher(line.removeColor()) {
                    pieces = group("pieces").toInt()
                }

                armorAbilityFortune.matchMatcher(line) {
                    return if (pieces < 2) 0.0 else group("bonus").toDouble() / pieces
                }
            }

            return 0.0
        }

        fun loadFortuneLineData(tool: ItemStack?, enchantmentFortune: Double) {
            displayedFortune = 0.0
            reforgeFortune = 0.0
            itemBaseFortune = 0.0
            greenThumbFortune = 0.0
            for (line in tool?.getLore()!!) {
                val match = tooltipFortunePattern.matchEntire(line)?.groups
                if (match != null) {
                    displayedFortune = match[1]!!.value.toDouble()
                    reforgeFortune = match[2]?.value?.toDouble() ?: 0.0

                    itemBaseFortune = if (tool.getInternalName().contains("LOTUS")) 5.0
                    else displayedFortune - reforgeFortune - enchantmentFortune - (tool.getFarmingForDummiesCount() ?: 0 ) * 1.0
                    greenThumbFortune = if (tool.getInternalName().contains("LOTUS")) {
                        displayedFortune - reforgeFortune - itemBaseFortune
                    } else 0.0
                }
            }
        }

        fun getCurrentFarmingFortune(alwaysBaseFortune: Boolean = false): Double {
            val upgradeFortune = upgradeFortune ?: 0.0
            val accessoryFortune = accessoryFortune ?: 0.0

            val baseFortune = if (alwaysBaseFortune) 100.0 else baseFortune
            var otherFortune = 0.0

            if (currentCrop == CropType.CARROT) {
                GardenAPI.config?.fortune?.let {
                    if (it.carrotFortune) otherFortune = 12.0
                }
            }
            if (currentCrop == CropType.PUMPKIN) {
                GardenAPI.config?.fortune?.let {
                    if (it.pumpkinFortune) otherFortune = 12.0
                }
            }
            return baseFortune + upgradeFortune + tabFortune + toolFortune + accessoryFortune + otherFortune
        }

        fun CropType.getLatestTrueFarmingFortune() = latestFF?.get(this)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3,"garden.farmingFortuneDisplay", "garden.farmingFortunes.display")
        event.move(3,"garden.farmingFortuneDropMultiplier", "garden.farmingFortunes.dropMultiplier")
        event.move(3,"garden.farmingFortunePos", "garden.farmingFortunes.pos")
    }
}
