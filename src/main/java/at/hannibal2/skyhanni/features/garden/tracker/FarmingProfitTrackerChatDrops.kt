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
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FarmingProfitTrackerChatDrops {

    private val patternGroup = RepoPattern.group("garden.farming.profit.tracker")
    private val overclocker = "OVERCLOCKER_3000".toInternalName()
    private val wildStrawberryDye = "DYE_WILD_STRAWBERRY".toInternalName()

    /**
     * REGEX-TEST: ABOUT TIME! You find an Overclocker 3000 (+137)!
     * REGEX-TEST: ABOUT TIME! You find an Overclocker 3000 (+1,810)!
     */
    private val overclockerPityPattern by patternGroup.pattern(
        "overclocker.pity",
        "^ABOUT TIME! You find an Overclocker 3000 \\([+]\\d[\\d,.]*" +
            "[${FARMING_FORTUNE.hypixelIcon}${OVERBLOOM.hypixelIcon}]\\)!$",
    )

    /**
     * REGEX-TEST: WOW! [MVP+] Eisengolem found a Wild Strawberry Dye!
     */
    private val dyeDropPattern by patternGroup.pattern(
        "dye.drop",
        "^WOW! (?<player>.+) found an? (?<item>.+ Dye)!$",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (overclockerPityPattern.matches(event.cleanMessage)) {
            FarmingProfitTracker.addPestItem(overclocker, 1)
        }
        dyeDropPattern.matchMatcher(event.cleanMessage) {
            if (!group("player").endsWith(PlayerUtils.getName())) return@matchMatcher
            if (group("item") != "Wild Strawberry Dye") return@matchMatcher
            FarmingProfitTracker.addRareCropItem(wildStrawberryDye, RareCropDropType.WILD_STRAWBERRY_DYE)
        }
    }
}
