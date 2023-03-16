package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
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
    private val cropNames = mutableMapOf<String, String>() // internalName -> cropName
    private var hasCropInHand = false

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.moneyPerHourPos.renderStringsAndItems(display)
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val crop = if (event.isRealCrop) event.crop else null
        hasCropInHand = crop != null
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

        if (ready) {
            val newDisplay = drawNewDisplay()
            display.clear()
            display.addAll(newDisplay)
        }
    }

    private fun drawNewDisplay(): MutableList<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()

        if (!hasCropInHand && !config.moneyPerHourAlwaysOn) return newDisplay

        newDisplay.add(Collections.singletonList("§7Money per hour when selling:"))

        var number = 0
        for ((internalName, moneyPerHour) in calculateMoneyPerHour().sortedDesc()) {
            number++
            val cropName = cropNames[internalName]
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
            list.add("$color$itemName§7: §6$format")

            newDisplay.add(list)
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

            val ignoreCheapCraftedItems = listOf(
                "BREAD",
                "BUILDER_BROWN_MUSHROOM",
                "BUILDER_CACTUS",
                "BUILDER_MELON",
                "CACTUS_BOOTS",
                "CACTUS_CHESTPLATE",
                "CACTUS_HELMET",
                "CACTUS_LEGGINGS",
                "FARM_SUIT_BOOTS",
                "FARM_SUIT_CHESTPLATE",
                "FARM_SUIT_HELMET",
                "FARM_SUIT_LEGGINGS",
                "MUSHROOM_BOOTS",
                "MUSHROOM_CHESTPLATE",
                "MUSHROOM_HELMET",
                "MUSHROOM_LEGGINGS",
                "PAPER",
                "POTION_AFFINITY_TALISMAN",
                "PUMPKIN_BOOTS",
                "PUMPKIN_CHESTPLATE",
                "PUMPKIN_HELMET",
                "PUMPKIN_LEGGINGS",
                "SIMPLE_CARROT_CANDY",
                "SPEED_TALISMAN",
            )

            val ignoreCheapItems = listOf(
                "BROWN_MUSHROOM",
                "CACTUS",
                "CARROT_ITEM",
                "ENCHANTED_BREAD",
                "HAY_BLOCK",
                "HUGE_MUSHROOM_1",
                "HUGE_MUSHROOM_2",
                "INK_SACK-3",
                "MELON",
                "MELON_BLOCK",
                "NETHER_STALK",
                "POTATO_ITEM",
                "PUMPKIN",
                "RED_MUSHROOM",
                "SUGAR_CANE",
                "WHEAT",
            )

            for ((internalName, _) in NotEnoughUpdates.INSTANCE.manager.itemInformation) {
                if (ignoreCheapCraftedItems.contains(internalName)) continue
                if (ignoreCheapItems.contains(internalName)) continue
                // filter craftable items
                if (internalName.endsWith("_BOOTS") ||
                    internalName.endsWith("_HELMET") ||
                    internalName.endsWith("_LEGGINGS") ||
                    internalName.endsWith("_CHESTPLATE") ||
                    internalName == "ENCHANTED_PAPER"
                ) {
                    continue
                }

                val (newId, amount) = NEUItems.getMultiplier(internalName)
                val itemName = NEUItems.getItemStack(newId).name?.removeColor() ?: continue
                val cropName = GardenAPI.itemNameToCropName(itemName)
                if (crops.contains(cropName)) {
                    multipliers[internalName] = amount
                    cropNames[internalName] = cropName
                }
            }


            ready = true
            update()
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.moneyPerHourDisplay
}