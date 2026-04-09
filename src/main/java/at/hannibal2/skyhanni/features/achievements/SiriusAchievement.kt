package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object SiriusAchievement {

    /**
     * REGEX-TEST: [NPC] Lucius: You've purchased 15 items from the Dark Auction.
     */
    private val daItemsPattern by AchievementManager.group.pattern(
        "da-item-count",
        "\\[NPC] Lucius: You've purchased (?<count>\\d+) items from the Dark Auction.",
    )
    private const val DA_ACHIEVEMENT = "20 items da"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Adopted by Sirius".asComponent(),
            "Buy 20 items from Sirius".asComponent(),
            20f,
        )
        event.register(achievement, DA_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        daItemsPattern.matchMatcher(event.cleanMessage) {
            if (group("count").toInt() >= 20) {
                AchievementManager.completeAchievement(DA_ACHIEVEMENT)
            }
        }
    }
}
