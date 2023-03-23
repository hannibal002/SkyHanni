package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class CropMoneyDisplay {
    private val display = mutableListOf<List<Any>>()
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

        config.moneyPerHourPos.renderStringsAndItems(display)
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

        val newDisplay = drawNewDisplay()
        display.clear()
        display.addAll(newDisplay)
    }

    private fun drawNewDisplay(): MutableList<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()

        if (!ready) {
            newDisplay.add(Collections.singletonList("§7Money per hour when selling:"))
            newDisplay.add(Collections.singletonList("§eLoading..."))
            return newDisplay
        }

        if (!hasCropInHand && !config.moneyPerHourAlwaysOn) return newDisplay

        newDisplay.add(Collections.singletonList("§7Money per hour when selling:"))

        var number = 0
        val map = calculateMoneyPerHour()
        if (map.isEmpty()) {
            newDisplay.add(Collections.singletonList("§cFarm crops to add them to this list!"))
        } else {
            for ((internalName, moneyPerHour) in map.sortedDesc()) {
                number++
                val cropName = cropNames[internalName]!!
                val isCurrent = cropName == GardenAPI.cropInHand
                if (number > config.moneyPerHourShowOnlyBest && !isCurrent) continue

                val list = mutableListOf<Any>()
                list.add("§7$number# ")

                try {
                    val itemStack = NEUItems.getItemStack(internalName)
                    list.add(itemStack)
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
                val format = LorenzUtils.formatInteger(moneyPerHour.toLong())
                val itemName = NEUItems.getItemStack(internalName).name?.removeColor() ?: continue
                val color = if (isCurrent) "§e" else "§7"
                val contestFormat = if (GardenNextJacobContest.isNextCrop(cropName)) "§n" else ""
                list.add("$color$contestFormat$itemName§7: §6$format")

                newDisplay.add(list)
            }
        }

        return newDisplay
    }

    private fun calculateMoneyPerHour(): MutableMap<String, Double> {
        val moneyPerHours = mutableMapOf<String, Double>()
        for ((internalName, amount) in multipliers) {
            val price = NEUItems.getPrice(internalName)
            val cropName = cropNames[internalName]!!
            val speed = GardenAPI.getCropsPerSecond(cropName)
            // No speed data for item in hand
            if (speed == -1) continue

            // Price not found
            if (price == -1.0) continue

            val speedPerHr = speed.toDouble() * 60 * 60
            val blocksPerHour = speedPerHr / amount.toDouble()
            val moneyPerHour = price * blocksPerHour
            moneyPerHours[internalName] = moneyPerHour
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
            val crops = listOf(
                "Wheat",
                "Carrot",
                "Potato",
                "Pumpkin",
                "Sugar Cane",
                "Melon",
                "Cactus",
                "Cocoa Beans",
                "Mushroom",
                "Nether Wart",
            )

            for ((internalName, _) in NotEnoughUpdates.INSTANCE.manager.itemInformation) {
                if (!BazaarApi.isBazaarItem(internalName)) continue
                if (internalName == "ENCHANTED_PAPER") continue

                val (newId, amount) = NEUItems.getMultiplier(internalName)
                if (amount < 10) continue
                val itemName = NEUItems.getItemStack(newId).name?.removeColor() ?: continue
                val crop = GardenAPI.itemNameToCropName(itemName)
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