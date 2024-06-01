package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyFound
import at.hannibal2.skyhanni.utils.RegexUtils.findFirst
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SuperpairsClicksAlert {

    private val config get() = SkyHanniMod.feature.inventory.helper.enchanting

    private val patternGroup = RepoPattern.group("superpairsclickalert")
    private val roundsNeededPattern by patternGroup.pattern(
        "roundsneeded",
        "(?:Chain|Series) of (?<roundsneeded>\\d+):"
    )
    private val currentRoundPattern by patternGroup.pattern(
        "currentround",
        "Round: (?<round>\\d+)"
    )
    private val tooLowPattern by patternGroup.pattern(
        "toolow",
        "Enchanting level too low!|Not enough experience!"
    )
    private val practiceModePattern by patternGroup.pattern(
        "practicemode",
        "Practice mode has no rewards"
    )

    private var roundsNeeded = -1
    private val targetInventoryNames = arrayOf("Chronomatron", "Ultrasequencer")

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!config.superpairsClicksAlert) return
        if (!targetInventoryNames.any { event.inventoryName.contains(it) }) return

        // player may have drank Metaphysical Serum which reduces clicks needed by up to 3, so need to parse it
        for (i in 24 downTo 20) {
            val lore = event.inventoryItems[i]?.getLore() ?: continue
            if (practiceModePattern.anyFound(lore)) {
                roundsNeeded = -1
                break
            }
            if (tooLowPattern.anyFound(lore)) continue
            lore.asReversed().findFirst(roundsNeededPattern) {
                roundsNeeded = group("roundsneeded").toInt()
            }
            break
        }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!config.superpairsClicksAlert) return
        if (roundsNeeded == -1) return
        if (!targetInventoryNames.any { event.inventoryName.contains(it) }) return

        val currentRounds = event.inventoryItems[4]?.displayName?.removeColor()?.let {
            currentRoundPattern.findMatcher(it) {
                group("round").toInt()
            }
        } ?: -1

        // checks if we have succeeded in either minigame
        if ((event.inventoryName.contains("Chronomatron")
                && (currentRounds > roundsNeeded)) || (event.inventoryName.contains("Ultrasequencer")
                && event.inventoryItems.entries
                .filter { it.key < 45 }
                .any { it.value.stackSize > roundsNeeded })
        ) {
            SoundUtils.playBeepSound()
            ChatUtils.chat("You have reached the maximum extra Superpairs clicks from this add-on!")
            roundsNeeded = -1
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(46, "misc.superpairsClicksAlert", "inventory.helper.enchanting.superpairsClicksAlert")
    }
}
