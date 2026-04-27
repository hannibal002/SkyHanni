package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.experimentationtable.ExperimentsAddonsConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsAddonsHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.SoundUtils

@SkyHanniModule
object SuperpairsSequenceAlert {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    private var roundsNeededForMaxClicks = -1
    private val roundsNeededRegex = Regex("""(?:Chain|Series) of (\d+):""")

    private var chronomatronRoundsNeededForMaxXP = -1
    private var ultraSequencerRoundsNeededForMaxXP = -1

    private var chronomatronRoundXPCap = 15
    private var ultraSequencerRoundXPCap = 20

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val sequenceType = config.addons.maxSequenceAlertType
        if (sequenceType == ExperimentsAddonsConfig.MaxSequenceAlertType.OFF) return
        if (!event.inventoryName.endsWith("Stakes")) return

        // player may have drank Metaphysical Serum which reduces clicks needed by up to 3, so need to parse it
        for (i in 24 downTo 20) {
            val lore = event.inventoryItems[i]?.getLoreComponent() ?: continue
            if (lore.any { it.string.contains("Practice mode has no rewards") }) {
                roundsNeededForMaxClicks = -1
                chronomatronRoundsNeededForMaxXP = -1
                ultraSequencerRoundsNeededForMaxXP = -1
                break
            }

            if (lore.any { it.string.contains("Enchanting level too low!") || it.string.contains("Not enough experience!") }) continue

            when (sequenceType) {
                ExperimentsAddonsConfig.MaxSequenceAlertType.MAX_CLICKS -> {
                    val match = lore.asReversed().firstNotNullOfOrNull { roundsNeededRegex.find(it.string) } ?: continue
                    roundsNeededForMaxClicks = match.groups[1]!!.value.toInt()
                    break
                }

                ExperimentsAddonsConfig.MaxSequenceAlertType.MAX_XP -> {
                    chronomatronRoundsNeededForMaxXP = chronomatronRoundXPCap
                    ultraSequencerRoundsNeededForMaxXP = ultraSequencerRoundXPCap
                    break
                }

                ExperimentsAddonsConfig.MaxSequenceAlertType.OFF -> break
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!ExperimentationTableApi.inAddon) return

        when (config.addons.maxSequenceAlertType) {
            ExperimentsAddonsConfig.MaxSequenceAlertType.MAX_CLICKS -> {
                if (roundsNeededForMaxClicks == -1) return
                if (!hasReachedRound(roundsNeededForMaxClicks, roundsNeededForMaxClicks)) return

                SoundUtils.playBeepSound()
                ChatUtils.chat("You have reached the maximum extra Superpairs clicks from this add-on!")
                roundsNeededForMaxClicks = -1
            }

            ExperimentsAddonsConfig.MaxSequenceAlertType.MAX_XP -> {
                if (chronomatronRoundsNeededForMaxXP == -1 || ultraSequencerRoundsNeededForMaxXP == -1) return
                if (!hasReachedRound(chronomatronRoundsNeededForMaxXP, ultraSequencerRoundsNeededForMaxXP)) return

                SoundUtils.playBeepSound()
                ChatUtils.chat("You have reached the maximum Enchanting XP from this add-on!")
                chronomatronRoundsNeededForMaxXP = -1
                ultraSequencerRoundsNeededForMaxXP = -1
            }

            ExperimentsAddonsConfig.MaxSequenceAlertType.OFF -> return
        }
    }

    private fun hasReachedRound(chronomatronRoundsNeeded: Int, ultraSequencerRoundsNeeded: Int): Boolean {
        return when {
            ExperimentationTableApi.inChronomatron -> {
                ExperimentsAddonsHelper.currentChronomatronRound > chronomatronRoundsNeeded
            }

            ExperimentationTableApi.inUltrasequencer -> {
                // We subtract 1 due to a Hypixel bug causing one less round to be required
                ExperimentsAddonsHelper.currentUltraSequencerRound > (ultraSequencerRoundsNeeded - 1)
            }

            else -> false
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(46, "misc.superpairsClicksAlert", "inventory.helper.enchanting.superpairsClicksAlert")

        event.move(59, "inventory.helper.enchanting.superpairsClicksAlert", "inventory.experimentationTable.superpairsClicksAlert")

        val pathBase = "inventory.experimentationTable"
        event.move(93, "$pathBase.superpairsClicksAlert", "$pathBase.addons.maxSequenceAlert")

        event.move(132, "$pathBase.addons.maxSequenceAlert", "$pathBase.addons.maxSequenceAlertType") {
            ConfigUtils.migrateBooleanToEnum(
                it, ExperimentsAddonsConfig.MaxSequenceAlertType.MAX_CLICKS, ExperimentsAddonsConfig.MaxSequenceAlertType.OFF,
            )
        }
    }
}
