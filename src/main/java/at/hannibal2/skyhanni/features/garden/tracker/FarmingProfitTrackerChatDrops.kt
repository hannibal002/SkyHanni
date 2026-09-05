package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat.FARMING_FORTUNE
import at.hannibal2.skyhanni.data.model.SkyblockStat.OVERBLOOM
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.tracker.RareCropTracker.RareCropDropType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches

@SkyHanniModule
object FarmingProfitTrackerChatDrops {

    private val overclocker = "OVERCLOCKER_3000".toInternalName()
    private val wildStrawberryDye = "DYE_WILD_STRAWBERRY".toInternalName()

    /**
     * REGEX-TEST: ABOUT TIME! You find an Overclocker 3000 (+137)!
     * REGEX-TEST: ABOUT TIME! You find an Overclocker 3000 (+1,810)!
     */
    private val overclockerPityPattern by FarmingProfitTracker.patternGroup.pattern(
        "overclocker.pity",
        "^ABOUT TIME! You find an Overclocker 3000 \\([+]\\d[\\d,.]*" +
            "[${FARMING_FORTUNE.hypixelIcon}${OVERBLOOM.hypixelIcon}]\\)!$",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (overclockerPityPattern.matches(event.cleanMessage)) {
            FarmingProfitTracker.addPestItem(overclocker, 1)
        }
        RareCropTracker.dyeDropPattern.matchMatcher(event.cleanMessage) {
            if (group("player") != PlayerUtils.getName()) return@matchMatcher
            if (group("item") != "Wild Strawberry Dye") return@matchMatcher
            FarmingProfitTrackerCrops.addRareCropItem(wildStrawberryDye, RareCropDropType.WILD_STRAWBERRY_DYE)
        }
    }
}
