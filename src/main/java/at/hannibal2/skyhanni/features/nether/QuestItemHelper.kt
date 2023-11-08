package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.hours

class QuestItemHelper {
    private val config get() = SkyHanniMod.feature.crimsonIsle

    private val itemCollectionPattern = ". (?<name>[\\w ]+) x(?<amount>\\d+)".toPattern()
    private var lastSentMessage = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Fetch") return
        if (lastSentMessage.passedSince() < 1.hours) return

        for ((_, item) in event.inventoryItems) {
            val (questItem, need) = itemCollectionPattern.matchMatcher(item.displayName.removeColor()) {
                group("name") to group("amount").toInt()
            } ?: continue

            val have = InventoryUtils.countItemsInLowerInventory { it.name?.contains(questItem) == true }
            if (have >= need) break

            val missingAmount = need - have
            LorenzUtils.clickableChat(
                "Click here to grab x$missingAmount $questItem from sacks!",
                "gfs $questItem $missingAmount"
            )
            lastSentMessage = SimpleTimeMark.now()
            break
        }
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.questItemHelper
}
