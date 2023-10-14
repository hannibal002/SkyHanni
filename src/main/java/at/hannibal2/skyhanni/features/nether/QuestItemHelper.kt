package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.hours

class QuestItemHelper {

    private val itemCollectionPattern = ". (?<name>[\\w ]+) x(?<amount>\\d+)".toPattern()
    private var questItem = ""
    private var questAmount = 0
    private var lastSentMessage = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!SkyHanniMod.feature.crimsonIsle.crimsonQuestItems) return
        if (event.inventoryName != "Fetch") return
        if (lastSentMessage.passedSince() < 1.hours) return
        loop@ for ((_, item) in event.inventoryItems) {
            itemCollectionPattern.matchMatcher(item.displayName.removeColor()) {
                if (!matches()) continue@loop
                questItem = group("name")
                questAmount = group("amount").toInt()
                LorenzUtils.clickableChat(
                    "§e[SkyHanni] Get x$questAmount $questItem from sacks",
                    "gfs $questItem $questAmount"
                )
                lastSentMessage = SimpleTimeMark.now()
                break@loop
            }
        }
    }
}
