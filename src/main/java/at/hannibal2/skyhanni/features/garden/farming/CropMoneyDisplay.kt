package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.bazaar.BazaarData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.isSpeedDataEmpty
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.moveEntryToTop
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object CropMoneyDisplay {
    var multipliers = mapOf<String, Int>()
    private var showCalculation = false
    private val melonDicer = mutableListOf<Int>()
    private val pumpkinDicer = mutableListOf<Int>()


    fun toggleShowCalculation() {
        showCalculation = !showCalculation
        LorenzUtils.chat("§e[SkyHanni] Show crop money calculation: " + if (showCalculation) "enabled" else "disabled")
        update()
    }

    private var display = emptyList<List<Any>>()
    private val config get() = SkyHanniMod.feature.garden
    private var loaded = false
    private var ready = false
    private val cropNames = mutableMapOf<String, CropType>() // internalName -> cropName
    private val toolHasBountiful get() = GardenAPI.config?.toolWithBountiful

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        if (!GardenAPI.hideExtraGuis()) {
            config.moneyPerHourPos.renderStringsAndItems(display, posLabel = "Garden Crop Money Per Hour")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        update()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(5)) return

        if (GardenAPI.getCurrentlyFarmedCrop() == null && !config.moneyPerHourAlwaysOn) return

        update()
    }

    private fun update() {
        init()

        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()

        val title = if (config.moneyPerHourCompact) {
            "§7Money/Hour:"
        } else {
            "§7Money per Hour when selling:"
        }

        if (!ready) {
            newDisplay.addAsSingletonList(title)
            newDisplay.addAsSingletonList("§eLoading...")
            return newDisplay
        }

        if (GardenAPI.getCurrentlyFarmedCrop() == null && !config.moneyPerHourAlwaysOn) return newDisplay

        newDisplay.addAsSingletonList(fullTitle(title))

        if (!config.cropMilestoneProgress) {
            newDisplay.addAsSingletonList("§cCrop Milestone Progress Display is disabled!")
            return newDisplay
        }

        var extraMushroomCowPerk = 0.0
        GardenAPI.getCurrentlyFarmedCrop()?.let {
            val reforgeName = InventoryUtils.getItemInHand()?.getReforgeName()
            toolHasBountiful?.put(it, reforgeName == "bountiful")

            if (GardenAPI.mushroomCowPet && it != CropType.MUSHROOM) {
                val (redPrice, brownPrice) = if (LorenzUtils.noTradeMode) {
                    val redPrice = (BazaarApi.getBazaarDataByInternalName("ENCHANTED_RED_MUSHROOM")?.npcPrice ?: 160.0) / 160
                    val brownPrice = (BazaarApi.getBazaarDataByInternalName("ENCHANTED_BROWN_MUSHROOM")?.npcPrice ?: 160.0) / 160
                    redPrice to brownPrice
                } else {
                    val redPrice = NEUItems.getPrice("ENCHANTED_RED_MUSHROOM") / 160
                    val brownPrice = NEUItems.getPrice("ENCHANTED_BROWN_MUSHROOM") / 160
                    redPrice to brownPrice
                }

                val mushroomPrice = (redPrice + brownPrice) / 2
                val perSecond = 20.0 * it.multiplier * mushroomPrice
                extraMushroomCowPerk = perSecond * 60 * 60
            }
        }

        val moneyPerHourData = calculateMoneyPerHour(newDisplay)
        if (moneyPerHourData.isEmpty()) {
            if (!isSpeedDataEmpty()) {
                val message = "money/hr empty but speed data not empty, retry"
                LorenzUtils.debug(message)
                println(message)
                newDisplay.addAsSingletonList("§eStill Loading...")
                ready = false
                loaded = false
                return newDisplay
            }
            newDisplay.addAsSingletonList("§cFarm crops to add them to this list!")
            return newDisplay
        }

        var number = 0
        val help = moneyPerHourData.mapValues { (_, value) -> value.max() }
        for (internalName in help.sortedDesc().keys) {
            number++
            val crop = cropNames[internalName]!!
            val isCurrent = crop == GardenAPI.getCurrentlyFarmedCrop()
            if (number > config.moneyPerHourShowOnlyBest && (!config.moneyPerHourShowCurrent || !isCurrent)) continue
            val debug = isCurrent && showCalculation
            if (debug) {
                newDisplay.addAsSingletonList("final calculation for: $internalName/$crop")
            }

            val list = mutableListOf<Any>()
            if (!config.moneyPerHourCompact) {
                list.add("§7$number# ")
            }

            try {
                if (isSeeds(internalName)) {
                    list.add(NEUItems.getItemStack("BOX_OF_SEEDS", true))
                } else {
                    list.add(NEUItems.getItemStack(internalName))
                }

                if (cropNames[internalName] == CropType.WHEAT && config.moneyPerHourMergeSeeds) {
                    list.add(NEUItems.getItemStack("BOX_OF_SEEDS", true))
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            if (!config.moneyPerHourCompact) {
                val itemName = NEUItems.getItemStack(internalName).name?.removeColor() ?: continue
                val currentColor = if (isCurrent) "§e" else "§7"
                val contestFormat = if (GardenNextJacobContest.isNextCrop(crop)) "§n" else ""
                list.add("$currentColor$contestFormat$itemName§7: ")
            }

            val coinsColor = if (isCurrent && config.moneyPerHourCompact) "§e" else "§6"
            val moneyArray = moneyPerHourData[internalName]!!

            for (price in moneyArray) {
                val finalPrice = price + extraMushroomCowPerk
                val format = format(finalPrice)
                if (debug) {
                    newDisplay.addAsSingletonList(" price: ${price.addSeparators()}")
                    newDisplay.addAsSingletonList(" extraMushroomCowPerk: ${extraMushroomCowPerk.addSeparators()}")
                    newDisplay.addAsSingletonList(" finalPrice: ${finalPrice.addSeparators()}")
                }
                list.add("$coinsColor$format")
                list.add("§7/")
            }
            list.removeLast()

            newDisplay.add(list)
        }
        return if (config.moneyPerHourHideTitle) newDisplay.drop(1) else newDisplay
    }

    private fun fullTitle(title: String): String {
        val titleText: String
        val nameList = mutableListOf<String>()
        if (config.moneyPerHourUseCustomFormat) {
            val map = mapOf(
                0 to "Sell Offer",
                1 to "Instant Sell",
                2 to "NPC Price",
            )
            val list = mutableListOf<String>()
            for (index in config.moneyPerHourCustomFormat) {
                map[index]?.let {
                    list.add(it)
                }
            }
            for (line in list) {
                nameList.add("§e$line")
                nameList.add("§7/")
            }
            nameList.removeLast()
            titleText = nameList.joinToString("")
        } else {
            titleText = if (LorenzUtils.noTradeMode) "§eNPC Price" else "§eSell Offer"
        }
        return "$title §7($titleText§7)"
    }

    private fun format(moneyPerHour: Double) = if (config.moneyPerHourCompactPrice) {
        NumberUtil.format(moneyPerHour)
    } else {
        LorenzUtils.formatInteger(moneyPerHour.toLong())
    }

    private fun calculateMoneyPerHour(debugList: MutableList<List<Any>>): Map<String, Array<Double>> {
        val moneyPerHours = mutableMapOf<String, Array<Double>>()

        var seedsPrice: BazaarData? = null
        var seedsPerHour = 0.0

        val onlyNpcPrice =
            (!config.moneyPerHourUseCustomFormat && LorenzUtils.noTradeMode) ||
                    (config.moneyPerHourUseCustomFormat && config.moneyPerHourCustomFormat.size == 1 &&
                            config.moneyPerHourCustomFormat[0] == 2)

        for ((internalName, amount) in multipliers.moveEntryToTop { isSeeds(it.key) }) {
            val crop = cropNames[internalName]!!
            // When only the NPC price is shown, display the price only for the base item
            if (onlyNpcPrice) {
                if (amount != 1) continue
            } else {
                if (amount < 10) {
                    continue
                }
            }

            var speed = crop.getSpeed()?.toDouble() ?: continue

            val isCurrent = crop == GardenAPI.getCurrentlyFarmedCrop()
            val debug = isCurrent && showCalculation
            if (debug) {
                debugList.addAsSingletonList("calculateMoneyPerHour: $internalName/$crop")
                debugList.addAsSingletonList(" speed: ${speed.addSeparators()}")
            }

            val isSeeds = isSeeds(internalName)
            if (debug) {
                debugList.addAsSingletonList(" isSeeds: $isSeeds")
            }
            if (isSeeds) speed *= 1.36
            if (crop.replenish) {
                val blockPerSecond = crop.multiplier * 20
                if (debug) {
                    debugList.addAsSingletonList(" replenish blockPerSecond reduction: ${blockPerSecond.addSeparators()}")
                }
                speed -= blockPerSecond
            }

            val speedPerHour = speed * 60 * 60
            if (debug) {
                debugList.addAsSingletonList(" speedPerHour: ${speedPerHour.addSeparators()}")
            }
            val cropsPerHour = speedPerHour / amount.toDouble()
            if (debug) {
                debugList.addAsSingletonList(" cropsPerHour: ${cropsPerHour.addSeparators()}")
            }

            val bazaarData = BazaarApi.getBazaarDataByInternalName(internalName) ?: continue

            var npcPrice = bazaarData.npcPrice * cropsPerHour
            var sellOffer = bazaarData.buyPrice * cropsPerHour
            var instantSell = bazaarData.sellPrice * cropsPerHour
            if (debug) {
                debugList.addAsSingletonList(" npcPrice: ${npcPrice.addSeparators()}")
                debugList.addAsSingletonList(" sellOffer: ${sellOffer.addSeparators()}")
                debugList.addAsSingletonList(" instantSell: ${instantSell.addSeparators()}")
            }

            if (crop == CropType.WHEAT && config.moneyPerHourMergeSeeds) {
                if (isSeeds) {
                    seedsPrice = bazaarData
                    seedsPerHour = cropsPerHour
                    continue
                } else {
                    seedsPrice?.let {
                        if (debug) {
                            debugList.addAsSingletonList(" added seedsPerHour: $seedsPerHour")
                        }
                        npcPrice += it.npcPrice * seedsPerHour
                        sellOffer += it.buyPrice * seedsPerHour
                        instantSell += it.sellPrice * seedsPerHour
                    }
                }
            }

            val bountifulMoney = if (toolHasBountiful?.get(crop) == true) speedPerHour * 0.2 else 0.0
            moneyPerHours[internalName] =
                formatNumbers(sellOffer + bountifulMoney, instantSell + bountifulMoney, npcPrice + bountifulMoney)
        }
        return moneyPerHours
    }

    private fun isSeeds(internalName: String) = (internalName == "ENCHANTED_SEEDS" || internalName == "SEEDS")

    private fun formatNumbers(sellOffer: Double, instantSell: Double, npcPrice: Double): Array<Double> {
        return if (config.moneyPerHourUseCustomFormat) {
            val map = mapOf(
                0 to sellOffer,
                1 to instantSell,
                2 to npcPrice,
            )
            val newList = mutableListOf<Double>()
            for (index in config.moneyPerHourCustomFormat) {
                map[index]?.let {
                    newList.add(it)
                }
            }
            newList.toTypedArray()
        } else {
            if (LorenzUtils.noTradeMode) {
                arrayOf(npcPrice)
            } else {
                arrayOf(sellOffer)
            }
        }
    }

    private fun calculateAverageDicer(dicerList: MutableList<Int>, dropsJson: JsonObject) {
        dicerList.clear()
        val totalChance = dropsJson["total chance"].asDouble
        val dropTypes = dropsJson["drops"].asJsonArray
        for (dropType in dropTypes) {
            val dropJson = dropType.asJsonObject
            val chance = (dropJson["chance"].asDouble / totalChance)
            dropJson["amount"].asJsonArray.forEachIndexed { index, element ->
                val amount = element.asInt * chance * 60 * 60
                if (index < dicerList.size) {
                    dicerList[index] += amount.toInt()
                } else {
                    dicerList.add(amount.toInt())
                }
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val dicerJson = event.getConstant("DicerDrops")!!
            calculateAverageDicer(melonDicer, dicerJson["MELON"].asJsonObject)
            calculateAverageDicer(pumpkinDicer, dicerJson["PUMPKIN"].asJsonObject)
        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    private fun init() {
        if (loaded) return
        loaded = true

        SkyHanniMod.coroutineScope.launch {
            val map = mutableMapOf<String, Int>()
            for ((internalName, _) in NEUItems.manager.itemInformation) {
                if (!BazaarApi.isBazaarItem(internalName)) continue
                if (internalName == "ENCHANTED_PAPER") continue
                if (internalName == "ENCHANTED_BREAD") continue
                if (internalName == "SIMPLE_CARROT_CANDY") continue
                if (internalName == "BOX_OF_SEEDS") continue

                val (newId, amount) = NEUItems.getMultiplier(internalName)
                val itemName = NEUItems.getItemStack(newId).name?.removeColor() ?: continue
                val crop = getByNameOrNull(itemName)
                crop?.let {
                    map[internalName] = amount
                    cropNames[internalName] = it
                }
            }

            multipliers = map

            ready = true
            update()
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.moneyPerHourDisplay
}
