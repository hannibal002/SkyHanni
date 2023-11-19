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
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
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
    private val tabFortuneUniversalPattern = " Farming Fortune: §r§6☘(?<fortune>\\d+)".toRegex()
    private val tabFortuneCropPattern = " (?<crop>Wheat|Carrot|Potato|Pumpkin|Sugar Cane|Melon|Cactus|Cocoa Beans|Mushroom|Nether Wart) Fortune: §r§6☘(?<fortune>\\d+)".toRegex()

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
        event.tabList.firstNotNullOfOrNull {
            tabFortuneUniversalPattern.matchEntire(it)?.groups?.get("fortune")?.value?.toDoubleOrNull()?.let { tabFortuneUniversal = it }
            tabFortuneCropPattern.matchEntire(it)?.groups?.let {
                it.get("crop")?.value?.let { currentCrop = CropType.getByNameOrNull(it) }
                it.get("fortune")?.value?.toDoubleOrNull()?.let { tabFortuneCrop = it }
            }
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastToolSwitch = System.currentTimeMillis()
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
        val currentCrop = currentCrop ?: return

        val displayCrop = GardenAPI.cropInHand ?: currentCrop
        var wrongTabCrop = false
        var farmingFortune = -1.0

        val updatedDisplay = mutableListOf<List<Any>>()
        updatedDisplay.add(mutableListOf<Any>().also {
            it.addCropIcon(displayCrop)

            var recentlySwitchedTool = System.currentTimeMillis() < lastToolSwitch + 1500
            wrongTabCrop = GardenAPI.cropInHand != null && GardenAPI.cropInHand != currentCrop

            if (wrongTabCrop) {
                farmingFortune = displayCrop.getLatestTrueFarmingFortune()?.let {
                    if (config.dropMultiplier) it else it - 100.0
                } ?: -1.0
                recentlySwitchedTool = false
            } else {
                farmingFortune = getCurrentFarmingFortune()
            }

            it.add(
                "§6Farming Fortune§7: §e" + if (!recentlySwitchedTool && farmingFortune != -1.0) {
                    LorenzUtils.formatDouble(farmingFortune, 0)
                } else "?"
            )

            if (GardenAPI.cropInHand == currentCrop) {
                latestFF?.put(currentCrop, getCurrentFarmingFortune(true))
            }
        })

        if (wrongTabCrop) {
            var text = "§cBreak a §e${GardenAPI.cropInHand?.cropName}§c to see"
            if (farmingFortune != -1.0) text += " latest"
            text += " fortune!"

            updatedDisplay.addAsSingletonList(text)
        }
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

    private fun isEnabled(): Boolean = GardenAPI.inGarden() && config.display

    companion object {
        private val config get() = SkyHanniMod.feature.garden.farmingFortunes
        private val latestFF: MutableMap<CropType, Double>? get() = GardenAPI.storage?.latestTrueFarmingFortune

        private var currentCrop: CropType? = null

        private var tabFortuneUniversal: Double = 0.0
        private var tabFortuneCrop: Double = 0.0
        private var toolFortune: Double = 0.0
        private val baseFortune: Double get() = if (config.dropMultiplier) 100.0 else 0.0
        private val upgradeFortune: Double? get() = currentCrop?.getUpgradeLevel()?.let { it * 5.0 }
        private val accessoryFortune: Double?
            get() = currentCrop?.let {
                CropAccessoryData.cropAccessory?.getFortune(it)
            }

        private val collectionPattern = "§7You have §6\\+(?<ff>\\d{1,3})☘ .*".toPattern()
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
            val string = tool?.getLore()?.nextAfter("§6Collection Analysis", 3) ?: return 0.0
            return collectionPattern.matchMatcher(string) { group("ff").toDoubleOrNull() } ?: 0.0
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
                val match = tooltipFortunePattern.matchEntire(line)?.groups ?: continue

                displayedFortune = match[1]!!.value.toDouble()
                reforgeFortune = match[2]?.value?.toDouble() ?: 0.0

                itemBaseFortune = if (tool.getInternalName().contains("LOTUS")) {
                    5.0
                } else {
                    val dummiesFF = (tool.getFarmingForDummiesCount() ?: 0) * 1.0
                    displayedFortune - reforgeFortune - enchantmentFortune - dummiesFF
                }
                greenThumbFortune = if (tool.getInternalName().contains("LOTUS")) {
                    displayedFortune - reforgeFortune - itemBaseFortune
                } else 0.0
            }
        }

        fun getCurrentFarmingFortune(alwaysBaseFortune: Boolean = false): Double {
            val baseFortune = if (alwaysBaseFortune) 100.0 else baseFortune
            return baseFortune + tabFortuneUniversal + tabFortuneCrop
        }

        fun CropType.getLatestTrueFarmingFortune() = latestFF?.get(this)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.farmingFortuneDisplay", "garden.farmingFortunes.display")
        event.move(3, "garden.farmingFortuneDropMultiplier", "garden.farmingFortunes.dropMultiplier")
        event.move(3, "garden.farmingFortunePos", "garden.farmingFortunes.pos")
    }
}
