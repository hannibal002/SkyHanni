package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.composter.ComposterAPI.getLevel
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.GardenJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.DurationUnit

class ComposterOverlay {
    private var organicMatterFactors: Map<String, Double> = emptyMap()
    private var fuelFactors: Map<String, Double> = emptyMap()

    private val config get() = SkyHanniMod.feature.garden
    private val hidden get() = SkyHanniMod.feature.hidden
    private var organicMatterDisplay = listOf<List<Any>>()
    private var fuelExtraDisplay = listOf<List<Any>>()

    private var currentOrganicMatterItem: String
        get() = hidden.gardenComposterCurrentOrganicMatterItem
        set(value) {
            hidden.gardenComposterCurrentOrganicMatterItem = value
        }

    private var currentFuelItem: String
        get() = hidden.gardenComposterCurrentFuelItem
        set(value) {
            hidden.gardenComposterCurrentFuelItem = value
        }

    private var currentTimeType = TimeType.HOUR
    private var inComposter = false
    private var inComposterUpgrades = false
    private var extraComposterUpgrade: ComposterUpgrade? = null
        set(value) {
            field = value
            lastHovered = System.currentTimeMillis()
        }

    private var maxLevel = false
    private var lastHovered = 0L

    companion object {
        var inInventory = false
    }

    var garden: GardenJson? = null

    @SubscribeEvent
    fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) {
        inInventory = false
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!inInventory) return

        update()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (inComposterUpgrades) {
            if (extraComposterUpgrade != null) {
//                if (System.currentTimeMillis() > lastHovered + 30) {
                if (System.currentTimeMillis() > lastHovered + 200) {
                    extraComposterUpgrade = null
                    update()
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.composterOverlay) return
        inComposter = event.inventoryName == "Composter"
        inComposterUpgrades = event.inventoryName == "Composter Upgrades"
        if (!inComposter && !inComposterUpgrades) return

        inInventory = true
        update()
    }

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (inComposterUpgrades) {
            update()
            for (upgrade in ComposterUpgrade.values()) {
                event.itemStack?.name?.let {
                    if (it.contains(upgrade.displayName)) {
                        val matcher = ComposterUpgrade.regex.matcher(it)
                        matcher.matches()
                        val level = matcher.group("level")?.romanToDecimalIfNeeded() ?: 0
                        maxLevel = level == 25
                        extraComposterUpgrade = upgrade
                        update()
                        return
                    }
                }
            }
            if (extraComposterUpgrade != null) {
                extraComposterUpgrade = null
                maxLevel = false
                update()
            }
        }
    }

    private fun update() {
        if (organicMatterFactors.isEmpty()) {
            organicMatterDisplay =
                Collections.singletonList(listOf("§cSkyHanni composter error:", "§cRepo data not loaded!"))
            return
        }

        if (inComposter) {
            organicMatterDisplay = drawOrganicMatterDisplay()
            fuelExtraDisplay = drawFuelExtraDisplay()
        } else if (inComposterUpgrades) {
            organicMatterDisplay = drawUpgradeStats()
            fuelExtraDisplay = emptyList()
        }
    }

    private fun drawUpgradeStats(): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()

        var upgrade = extraComposterUpgrade
        if (upgrade == null) {
            newList.addAsSingletonList("§7Preview: Nothing")
        } else {
            val level = upgrade.getLevel(null)
            val nextLevel = if (maxLevel) "§6§lMAX" else "§c➜ §a" + (level + 1)
            val displayName = upgrade.displayName
            newList.addAsSingletonList("§7Preview §a$displayName§7: §a$level $nextLevel")
        }
        newList.addAsSingletonList("")
        if (maxLevel) {
            upgrade = null
        }

        addExtraData(newList)

        val maxOrganicMatter = ComposterAPI.maxOrganicMatter(null)
        val maxOrganicMatterPreview = ComposterAPI.maxOrganicMatter(upgrade)

        val matterPer = ComposterAPI.organicMatterRequiredPer(null)
        val matterPerPreview = ComposterAPI.organicMatterRequiredPer(upgrade)

        val matterMaxDuration = ComposterAPI.timePerCompost(null) * (maxOrganicMatter / matterPer)
        val matterMaxDurationPreview =
            ComposterAPI.timePerCompost(upgrade) * (maxOrganicMatterPreview / matterPerPreview)

        var format = formatTime(matterMaxDuration)
        var formatPreview =
            if (matterMaxDuration != matterMaxDurationPreview) " §c➜ §b" + formatTime(matterMaxDurationPreview) else ""

        newList.addAsSingletonList("§7Full §eOrganic Matter §7empty time: §b$format$formatPreview")

        val maxFuel = ComposterAPI.maxFuel(null)
        val maxFuelPreview = ComposterAPI.maxFuel(upgrade)

        val fuelRequiredPer = ComposterAPI.fuelRequiredPer(null)
        val fuelRequiredPerPreview = ComposterAPI.fuelRequiredPer(upgrade)

        val fuelMaxDuration = ComposterAPI.timePerCompost(null) * (maxFuel / fuelRequiredPer)
        val fuelMaxDurationPreview =
            ComposterAPI.timePerCompost(upgrade) * (maxFuelPreview / fuelRequiredPerPreview)

        format = formatTime(fuelMaxDuration)
        formatPreview =
            if (fuelMaxDuration != fuelMaxDurationPreview) " §c➜ §b" + formatTime(fuelMaxDurationPreview) else ""
        newList.addAsSingletonList("§7Full §2Fuel §7empty time: §b$format$formatPreview")

        return newList
    }

    private fun formatTime(timePerCompost1: Duration) =
        TimeUtils.formatDuration(timePerCompost1.toLong(DurationUnit.MILLISECONDS), maxUnits = 2)

    private fun drawOrganicMatterDisplay(): MutableList<List<Any>> {
        val maxOrganicMatter = ComposterAPI.maxOrganicMatter(if (maxLevel) null else extraComposterUpgrade)
        val currentOrganicMatter = ComposterAPI.getOrganicMatter()
        val missingOrganicMatter = (maxOrganicMatter - currentOrganicMatter).toDouble()

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§7Items needed to fill §eOrganic Matter")
        val fillList = fillList(newList, organicMatterFactors, missingOrganicMatter) {
            currentOrganicMatterItem = it
            update()
        }
        if (currentOrganicMatterItem == "") {
            currentOrganicMatterItem = fillList
            update()
        }
        return newList
    }

    private fun drawFuelExtraDisplay(): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()

        addExtraData(newList)

        if (inComposter) {
            newList.addAsSingletonList("§7Items needed to fill §2Fuel")
            val maxFuel = ComposterAPI.maxFuel(null)
            val currentFuel = ComposterAPI.getFuel()
            val missingFuel = (maxFuel - currentFuel).toDouble()
            val fillList = fillList(newList, fuelFactors, missingFuel) {
                currentFuelItem = it
                update()
            }
            if (currentFuelItem == "") {
                currentFuelItem = fillList
                update()
            }
        }
        return newList
    }

    private fun addExtraData(newList: MutableList<List<Any>>) {
        val organicMatterItem = currentOrganicMatterItem
        val fuelItem = currentFuelItem
        if (organicMatterItem == "" || fuelItem == "") return

        val clickableList = mutableListOf<Any>()
        clickableList.add("§7Per ")
        for (type in TimeType.values()) {
            val display = type.display
            if (type == currentTimeType) {
                clickableList.add("§a[$display]")
            } else {
                clickableList.add("§e[")
                clickableList.add(Renderable.link("§e$display") {
                    currentTimeType = type
                    update()
                })
                clickableList.add("§e]")
            }
            clickableList.add(" ")
        }
        newList.add(clickableList)


        val list = mutableListOf<Any>()
        list.add("§7Using: ")
        list.add(NEUItems.getItemStack(organicMatterItem))
        list.add("§7and ")
        list.add(NEUItems.getItemStack(fuelItem))
        newList.add(list)

        val timePerCompost = ComposterAPI.timePerCompost(null).toLong(DurationUnit.MILLISECONDS)
        val upgrade = if (maxLevel) null else extraComposterUpgrade
        val timePerCompostPreview = ComposterAPI.timePerCompost(upgrade).toLong(DurationUnit.MILLISECONDS)
        val format = TimeUtils.formatDuration(timePerCompost)
        val formatPreview =
            if (timePerCompostPreview != timePerCompost) " §c➜ §b" + TimeUtils.formatDuration(timePerCompostPreview) else ""
        newList.addAsSingletonList(" §7Time per Compost: §b$format$formatPreview")

        val timeText = currentTimeType.display.lowercase()
        val timeMultiplier = if (currentTimeType != TimeType.COMPOST) {
            (currentTimeType.multiplier * 1000 / (timePerCompost.toDouble()))
        } else 1.0
        val timeMultiplierPreview = if (currentTimeType != TimeType.COMPOST) {
            (currentTimeType.multiplier * 1000 / (timePerCompostPreview.toDouble()))
        } else 1.0

        val multiDropFactor = ComposterAPI.multiDropChance(null) + 1
        val multiDropFactorPreview = ComposterAPI.multiDropChance(upgrade) + 1
        val multiplier = multiDropFactor * timeMultiplier
        val multiplierPreview = multiDropFactorPreview * timeMultiplierPreview
        val compostPerTitlePreview =
            if (multiplier != multiplierPreview) " §c➜ §e" + multiplierPreview.round(2) else ""
        val compostPerTitle =
            if (currentTimeType == TimeType.COMPOST) "Compost multiplier" else "Composts per $timeText"
        newList.addAsSingletonList(" §7$compostPerTitle: §e${multiplier.round(2)}$compostPerTitlePreview")


        val organicMatterPrice = getPrice(organicMatterItem)
        val organicMatterFactor = organicMatterFactors[organicMatterItem]!!

        val organicMatterRequired = ComposterAPI.organicMatterRequiredPer(null)
        val organicMatterRequiredPreview = ComposterAPI.organicMatterRequiredPer(upgrade)

        val organicMatterPricePer = organicMatterPrice * (organicMatterRequired / organicMatterFactor)
        val organicMatterPricePerPreview = organicMatterPrice * (organicMatterRequiredPreview / organicMatterFactor)

        val fuelPrice = getPrice(fuelItem)
        val fuelFactor = fuelFactors[fuelItem]!!

        val fuelRequired = ComposterAPI.fuelRequiredPer(null)
        val fuelRequiredPreview = ComposterAPI.fuelRequiredPer(upgrade)

        val fuelPricePer = fuelPrice * (fuelRequired / fuelFactor)
        val fuelPricePerPreview = fuelPrice * (fuelRequiredPreview / fuelFactor)

        val totalCost = (fuelPricePer + organicMatterPricePer) * timeMultiplier
        val totalCostPreview = (fuelPricePerPreview + organicMatterPricePerPreview) * timeMultiplierPreview

        val materialCostFormatPreview =
            if (totalCost != totalCostPreview) " §c➜ §6" + NumberUtil.format(totalCostPreview) else ""
        val materialCostFormat =
            " §7Material costs per $timeText: §6${NumberUtil.format(totalCost)}$materialCostFormatPreview"
        newList.addAsSingletonList(materialCostFormat)


        val priceCompost = getPrice("COMPOST")
        val profit = (priceCompost - (fuelPricePer + organicMatterPricePer)) * multiplier
        val profitPreview = (priceCompost - (fuelPricePerPreview + organicMatterPricePerPreview)) * multiplierPreview

        val profitFormatPreview = if (profit != profitPreview) " §c➜ §6" + NumberUtil.format(profitPreview) else ""
        val profitFormat = " §7Profit per $timeText: §6${NumberUtil.format(profit)}$profitFormatPreview"
        newList.addAsSingletonList(profitFormat)

        newList.addAsSingletonList("")
    }

    private fun fillList(
        bigList: MutableList<List<Any>>,
        factors: Map<String, Double>,
        missing: Double,
        onClick: (String) -> Unit,
    ): String {
        val map = mutableMapOf<String, Double>()
        for ((internalName, factor) in factors) {
            map[internalName] = factor / getPrice(internalName)
        }

        var i = 0
        var first: String? = null
        for (internalName in map.sortedDesc().keys) {
            if (first == null) first = internalName
            val factor = factors[internalName]!!

            val item = NEUItems.getItemStack(internalName)
            val itemName = item.name!!
            val price = getPrice(internalName)
            val itemsNeeded = ceil(missing / factor)
            val totalPrice = itemsNeeded * price

            val list = mutableListOf<Any>()
            list.add(item)
            val format = NumberUtil.format(totalPrice)
            val selected =
                if (internalName == currentOrganicMatterItem || internalName == currentFuelItem) "§n" else ""
            val rawItemName = itemName.removeColor()
            val name = itemName.substring(0, 2) + selected + rawItemName
            list.add(Renderable.link("$name§r §8x${itemsNeeded.addSeparators()} §7(§6$format§7)") {
                onClick(internalName)
                if (LorenzUtils.isControlKeyDown()) {
                    inInventory = false
                    LorenzUtils.sendCommandToServer("bz $rawItemName")
                    OSUtils.copyToClipboard("${itemsNeeded.toInt()}")
                }
            })
            bigList.add(list)


            i++
            if (i == 10) break
        }

        return first!!
    }

    private fun getPrice(internalName: String): Double {
        val price = NEUItems.getPrice(internalName)
        if (internalName == "BIOFUEL" && price > 20_000) return 20_000.0

        return price
    }

    @SubscribeEvent
    fun onRepoReload(event: io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent) {
        updateOrganicMatterFactors()
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        garden = event.getConstant<GardenJson>("Garden")!!
        updateOrganicMatterFactors()
    }

    private fun updateOrganicMatterFactors() {
        try {
            val garden = this.garden ?: return
            organicMatterFactors = updateOrganicMatterFactors(garden.organic_matter)
            fuelFactors = garden.fuel

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    private fun updateOrganicMatterFactors(baseValues: Map<String, Double>): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        for ((internalName, _) in NotEnoughUpdates.INSTANCE.manager.itemInformation) {
            if (internalName.endsWith("_BOOTS")) continue
            if (internalName.endsWith("_HELMET")) continue
            if (internalName.endsWith("_CHESTPLATE")) continue
            if (internalName.endsWith("_LEGGINGS")) continue
            if (internalName == "SPEED_TALISMAN") continue
            val (newId, amount) = NEUItems.getMultiplier(internalName)
            if (amount <= 9) continue
            val finalAmount =
                if (internalName == "ENCHANTED_HUGE_MUSHROOM_1" || internalName == "ENCHANTED_HUGE_MUSHROOM_2") {
                    //  160 * 8 * 4 is 5120 and not 5184, but hypixel made an error, so we have to copy the error
                    5184
                } else amount

            baseValues[newId]?.let {
                val d = it * finalAmount
                map[internalName] = d
            }
        }
        return map
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.composterOverlayOrganicMatterPos.renderStringsAndItems(
                organicMatterDisplay,
                posLabel = "Composter Overlay Organic Matter"
            )
            config.composterOverlayFuelExtrasPos.renderStringsAndItems(
                fuelExtraDisplay,
                posLabel = "Composter Overlay Fuel Extras"
            )
        }
    }

    enum class TimeType(val display: String, val multiplier: Int) {
        COMPOST("Compost", 1),
        HOUR("Hour", 60 * 60),
        DAY("Day", 60 * 60 * 24),
    }
}