package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.garden.GardenAPI.Companion.getSpeed
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
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
    private val multipliers = mutableMapOf<String, Int>()
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
            "§7Money/hour:"
        } else {
            "§7Money per hour when selling:"
        }

        if (!ready) {
            newDisplay.addAsSingletonList(title)
            newDisplay.addAsSingletonList("§eLoading...")
            return newDisplay
        }

        if (!hasCropInHand && !config.moneyPerHourAlwaysOn) return newDisplay


        newDisplay.addAsSingletonList(
            if (config.moneyPerHourAdvancedStats) {
                "$title §7(§eSell Offer§7/§eInstant Sell§7/§eNpc Price§7)"
            } else if (LorenzUtils.noTradeMode) {
                "$title §7(§eNpc Price§7)"
            } else {
                "$title §7(§eSell Offer§7)"
            }
        )

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
        val help = moneyPerHourData.mapValues  { (_, value) -> value.max() }
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
                list.add(NEUItems.getItemStack(internalName))
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
            if (config.moneyPerHourAdvancedStats) {
                for (price in moneyArray) {
                    val format = format(price)
                    list.add("$coinsColor$format")
                    list.add("§7/")
                }
                list.removeLast()
            } else if (LorenzUtils.noTradeMode) {
                // Show npc price
                val format = format(moneyArray[2])
                list.add("$coinsColor$format")
            } else {
                val format = format(moneyArray[0])
                list.add("$coinsColor$format")
            }


            newDisplay.add(list)
        }

        return newDisplay
    }

    private fun format(moneyPerHour: Double) = if (config.moneyPerHourCompactPrice) {
        NumberUtil.format(moneyPerHour)
    } else {
        LorenzUtils.formatInteger(moneyPerHour.toLong())
    }

    // sell offer -> instant sell -> npc
    private fun calculateMoneyPerHour(): Map<String, Array<Double>> {
        val moneyPerHours = mutableMapOf<String, Array<Double>>()
        for ((internalName, amount) in multipliers) {
            val crop = cropNames[internalName]!!
            val speed = crop.getSpeed()
            // No speed data for item in hand
            if (speed == -1) continue

            val speedPerHr = speed.toDouble() * 60 * 60
            val blocksPerHour = speedPerHr / amount.toDouble()

            val bazaarData = BazaarApi.getBazaarDataForInternalName(internalName) ?: continue

            val npcPrice = bazaarData.npcPrice * blocksPerHour
//            if (LorenzUtils.noTradeMode) {
//                moneyPerHours[internalName] = arrayOf(npcPrice)
//            } else {
            val sellOffer = bazaarData.buyPrice * blocksPerHour
            val instantSell = bazaarData.sellPrice * blocksPerHour
            moneyPerHours[internalName] = arrayOf(sellOffer, instantSell, npcPrice)
//            }

        }
        return moneyPerHours
    }

    private fun init() {
        if (loaded) return

        if (BazaarApi.bazaarMap.isEmpty()) {
            LorenzUtils.debug("bz not ready for money/time!")
            return
        }

        loaded = true

        SkyHanniMod.coroutineScope.launch {

            for ((internalName, _) in NotEnoughUpdates.INSTANCE.manager.itemInformation) {
                if (!BazaarApi.isBazaarItem(internalName)) continue
                if (internalName == "ENCHANTED_PAPER") continue

                val (newId, amount) = NEUItems.getMultiplier(internalName)
                if (amount < 10) continue
                val itemName = NEUItems.getItemStack(newId).name?.removeColor() ?: continue
                val crop = CropType.getByItemName(itemName)
                crop?.let {
                    multipliers[internalName] = amount
                    cropNames[internalName] = it
                }
            }


            ready = true
            update()
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.moneyPerHourDisplay
}
