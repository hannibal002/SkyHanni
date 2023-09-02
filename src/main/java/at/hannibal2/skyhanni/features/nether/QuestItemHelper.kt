package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.consoleLog
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class QuestItemHelper {

    private val itemCollectionPattern = ". (?<name>[\\w ]+) x(?<amount>\\d+)".toPattern()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!SkyHanniMod.feature.misc.crimsonQuestItems) return
        if (event.inventoryName != "Fetch") return
        loop@ for ((_, item) in event.inventoryItems) {
            itemCollectionPattern.matchMatcher(item.displayName.removeColor()) {
                if (!matches()) continue@loop
                consoleLog("Matched item: ${item.displayName.removeColor()}")
                LorenzUtils.sendCommandToServer("gfs ${group("name")} ${group("amount")}")
                break@loop
            }
        }
    }
}