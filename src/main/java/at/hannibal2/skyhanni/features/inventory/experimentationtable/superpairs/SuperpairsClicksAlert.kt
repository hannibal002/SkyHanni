package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsAddonsHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object SuperpairsClicksAlert {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    private var roundsNeeded = -1
    private val roundsNeededRegex = Regex("""(?:Chain|Series) of (\d+):""")

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!config.addons.maxSequenceAlert) return
        if (!event.inventoryName.endsWith("Stakes")) return

        // player may have drank Metaphysical Serum which reduces clicks needed by up to 3, so need to parse it
        for (i in 24 downTo 20) {
            val lore = event.inventoryItems[i]?.getLore() ?: continue
            if (lore.any { it.contains("Practice mode has no rewards") }) {
                roundsNeeded = -1
                break
            }
            if (lore.any { it.contains("Enchanting level too low!") || it.contains("Not enough experience!") }) continue
            val match = lore.asReversed().firstNotNullOfOrNull { roundsNeededRegex.find(it.removeColor()) } ?: continue
            roundsNeeded = match.groups[1]!!.value.toInt()
            break
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!config.addons.maxSequenceAlert) return
        if (!ExperimentationTableApi.inAddon || roundsNeeded == -1) return

        // Checks if we have succeeded in the applicable minigame
        val areWeDone = when {
            ExperimentationTableApi.inChronomatron -> {
                ExperimentsAddonsHelper.currentChronomatronRound > roundsNeeded
            }
            ExperimentationTableApi.inUltrasequencer -> {
                // We subtract 1 due to a Hypixel bug causing one less round to be required
                ExperimentsAddonsHelper.currentUltraSequencerRound > (roundsNeeded - 1)
            }
            else -> false
        }
        if (!areWeDone) return

        SoundUtils.playBeepSound()
        ChatUtils.chat("You have reached the maximum extra Superpairs clicks from this add-on!")
        roundsNeeded = -1
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(46, "misc.superpairsClicksAlert", "inventory.helper.enchanting.superpairsClicksAlert")

        event.move(59, "inventory.helper.enchanting.superpairsClicksAlert", "inventory.experimentationTable.superpairsClicksAlert")

        val pathBase = "inventory.experimentationTable"
        event.move(93, "$pathBase.superpairsClicksAlert", "$pathBase.addons.maxSequenceAlert")
    }
}
