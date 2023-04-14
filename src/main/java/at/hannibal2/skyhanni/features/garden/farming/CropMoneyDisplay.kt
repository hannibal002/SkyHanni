package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.bazaar.BazaarData
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getSpeed
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.moveEntryToTop
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CropMoneyDisplay {
    private var display = mutableListOf<List<Any>>()
    private val config get() = SkyHanniMod.feature.garden
    private var tick = 0
    private var loaded = false
    private var ready = false
    private var multipliers = mapOf<String, Int>()
    private val cropNames = mutableMapOf<String, CropType>() // internalName -> cropName
    private var hasCropInHand = false

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.moneyPerHourPos.renderStringsAndItems(display, posLabel = "Garden Crop Money Per Hour")
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        hasCropInHand = event.crop != null
        update()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % (20 * 5) != 0) return
        if (!hasCropInHand && !config.moneyPerHourAlwaysOn) return

        update()
    }

    private fun update() {
        init()

        display = drawDisplay()
    }

    private fun drawDisplay(): MutableList<List<Any>> {
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

        if (!hasCropInHand && !config.moneyPerHourAlwaysOn) return newDisplay

        newDisplay.addAsSingletonList(fullTitle(title))

        if (!config.cropMilestoneProgress) {
            newDisplay.addAsSingletonList("§cCrop Milestone Progress Display is disabled!")
            return newDisplay
        }

        val moneyPerHourData = calculateMoneyPerHour()
        if (moneyPerHourData.isEmpty()) {
            if (!GardenAPI.isSpeedDataEmpty()) {
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
            val cropName = cropNames[internalName]!!
            val isCurrent = cropName == GardenAPI.cropInHand
            if (number > config.moneyPerHourShowOnlyBest && !isCurrent) continue

            val list = mutableListOf<Any>()
            if (!config.moneyPerHourCompact) {
                list.add("§7$number# ")
            }

            try {
                if (internalName == "ENCHANTED_SEEDS") {
                    list.add(NEUItems.getItemStack("BOX_OF_SEEDS"))
                } else {
                    list.add(NEUItems.getItemStack(internalName))
                }

                if (cropNames[internalName] == CropType.WHEAT && config.moneyPerHourMergeSeeds) {
                    list.add(NEUItems.getItemStack("BOX_OF_SEEDS"))
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            if (!config.moneyPerHourCompact) {
                val itemName = NEUItems.getItemStack(internalName).name?.removeColor() ?: continue
                val currentColor = if (isCurrent) "§e" else "§7"
                val contestFormat = if (GardenNextJacobContest.isNextCrop(cropName)) "§n" else ""
                list.add("$currentColor$contestFormat$itemName§7: ")
            }

            val coinsColor = if (isCurrent && config.moneyPerHourCompact) "§e" else "§6"
            val moneyArray = moneyPerHourData[internalName]!!

            for (price in moneyArray) {
                val format = format(price)
                list.add("$coinsColor$format")
                list.add("§7/")
            }
            list.removeLast()

            newDisplay.add(list)
        }

        return newDisplay
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

    private fun calculateMoneyPerHour(): Map<String, Array<Double>> {
        val moneyPerHours = mutableMapOf<String, Array<Double>>()

        var seedsPrice: BazaarData? = null
        var seedsPerHour = 0.0

        for ((internalName, amount) in multipliers.moveEntryToTop { it.key == "ENCHANTED_SEEDS" }) {
            val crop = cropNames[internalName]!!
            var speed = crop.getSpeed().toDouble()
            if (speed == -1.0) continue
            val isSeeds = internalName == "ENCHANTED_SEEDS"
            if (isSeeds) speed *= 1.36
            if (crop.replenish) {
                val blockPerSecond = crop.multiplier * 20
                speed -= blockPerSecond
            }

            val speedPerHour = speed * 60 * 60
            val cropsPerHour = speedPerHour / amount.toDouble()

            val bazaarData = BazaarApi.getBazaarDataForInternalName(internalName) ?: continue

            var npcPrice = bazaarData.npcPrice * cropsPerHour
            var sellOffer = bazaarData.buyPrice * cropsPerHour
            var instantSell = bazaarData.sellPrice * cropsPerHour

            if (crop == CropType.WHEAT && config.moneyPerHourMergeSeeds) {
                if (isSeeds) {
                    seedsPrice = bazaarData
                    seedsPerHour = cropsPerHour
                    continue
                } else {
                    seedsPrice?.let {
                        npcPrice += it.npcPrice * seedsPerHour
                        sellOffer += it.buyPrice * seedsPerHour
                        instantSell += it.sellPrice * seedsPerHour
                    }
                }
            }

            moneyPerHours[internalName] = formatNumbers(sellOffer, instantSell, npcPrice)
        }
        return moneyPerHours
    }

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

    private fun init() {
        if (loaded) return

        if (BazaarApi.bazaarMap.isEmpty()) {
            LorenzUtils.debug("bz not ready for money/time!")
            return
        }

        loaded = true

        SkyHanniMod.coroutineScope.launch {
            val map = mutableMapOf<String, Int>()
            for ((internalName, _) in NotEnoughUpdates.INSTANCE.manager.itemInformation) {
                if (!BazaarApi.isBazaarItem(internalName)) continue
                if (internalName == "ENCHANTED_PAPER") continue
                if (internalName == "ENCHANTED_BREAD") continue

                val (newId, amount) = NEUItems.getMultiplier(internalName)
                if (amount < 10) continue
                val itemName = NEUItems.getItemStack(newId).name?.removeColor() ?: continue
                val crop = CropType.getByItemName(itemName)
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
