package at.hannibal2.skyhanni.features.misc.teleportpad

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TeleportPadInventoryNumber {

    private val numbers: Map<String, Int> by lazy {
        val baseNumber = mapOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10,
            "eleven" to 11,
            "twelve" to 12,
            "thirteen" to 13,
            "fourteen" to 14,
            "fifteen" to 15,
            "sixteen" to 16,
            "seventeen" to 17,
            "eighteen" to 18,
            "nineteen" to 19,
        )
        val multipliers = mapOf(
            "twenty" to 20,
            "thirty" to 30,
            "forty" to 40,
            "fifty" to 50,
            "sixty" to 60,
            "seventy" to 70,
            "eighty" to 80,
            "ninety" to 90,
        )

        val result = mutableMapOf<String, Int>()
        for (entry in baseNumber) {
            result[entry.key] = entry.value
        }

        for ((multiplyText, multiplyNumber) in multipliers) {
            result[multiplyText] = multiplyNumber
            for ((baseText, number) in baseNumber) {
                if (number > 9) continue
                result[multiplyText + baseText] = multiplyNumber + number
            }
        }
        result
    }

    private var inTeleportPad = false

    // TODO USE SH-REPO
    private val pattern = "§.(?<number>.*) teleport pad".toPattern()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inTeleportPad =
            event.inventoryName == "Set Destination" && SkyHanniMod.feature.misc.teleportPad.inventoryNumbers
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (!inTeleportPad) return

        val name = event.stack.name?.lowercase() ?: return

        pattern.matchMatcher(name) {
            val text = group("number")
            numbers[text]?.let {
                event.stackTip = "$it"
            }
        }
    }
}
