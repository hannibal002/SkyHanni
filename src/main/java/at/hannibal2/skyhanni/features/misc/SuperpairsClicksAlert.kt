package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SuperpairsClicksAlert {
    private val config get() = SkyHanniMod.feature.misc

    private var roundsNeeded = -1
    private val roundsNeededRegex = Regex("""(?:Chain|Series) of (\d+):""")
    private val currentRoundRegex = Regex("""Round: (\d+)""")
    private val targetInventoryNames = arrayOf("Chronomatron", "Ultrasequencer")

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!config.superpairsClicksAlert) return
        if (!targetInventoryNames.any { event.inventoryName.contains(it) }) return

        // player may have drank Metaphysical Serum which reduces clicks needed by up to 3, so need to parse it
        for (i in 24 downTo 20) {
            val lore = event.inventoryItems[i]?.getLore() ?: continue
            if (lore.any { it.contains("Practice mode has no rewards") }) {
                roundsNeeded = -1
                break
            }
            val match = lore.asReversed().firstNotNullOfOrNull { roundsNeededRegex.find(it.removeColor()) } ?: continue
            roundsNeeded = match.groups[1]!!.value.toInt()
            break
        }
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!config.superpairsClicksAlert) return
        if (roundsNeeded == -1) return
        if (!targetInventoryNames.any { event.inventoryName.contains(it) }) return

        if ( // checks if we have succeeded in either minigame
            (event.inventoryName.contains("Chronomatron")
                && ((event.inventoryItems[4]?.displayName?.removeColor()
                ?.let { currentRoundRegex.find(it) }
                ?.groups?.get(1)?.value?.toInt() ?: -1) > roundsNeeded))

            || (event.inventoryName.contains("Ultrasequencer")
                && event.inventoryItems.entries
                .filter { it.key < 45 }
                .any { it.value.stackSize > roundsNeeded })
        ) {
            SoundUtils.playBeepSound()
            LorenzUtils.chat("§e[SkyHanni] You have reached the maximum possible clicks!")
            roundsNeeded = -1
        }
    }
}
