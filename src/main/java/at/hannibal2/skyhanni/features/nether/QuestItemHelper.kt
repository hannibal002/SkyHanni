package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
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
    private var questItem = ""
    private var questAmount = 0
    private var lastSentMessage = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Fetch") return
        if (lastSentMessage.passedSince() < 1.hours) return
        items@ for ((_, item) in event.inventoryItems) {
            itemCollectionPattern.matchMatcher(item.displayName.removeColor()) {
                if (!matches()) continue@items
                questItem = group("name")
                questAmount = group("amount").toInt()
                LorenzUtils.clickableChat(
                    "§e[SkyHanni] Click here to grab x$questAmount $questItem from sacks!",
                    "gfs $questItem $questAmount"
                )
                lastSentMessage = SimpleTimeMark.now()
                break@items
            }
        }
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.questItemHelper
}
