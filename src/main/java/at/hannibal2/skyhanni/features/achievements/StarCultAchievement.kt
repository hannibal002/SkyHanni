package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object StarCultAchievement {

    /**
     * REGEX-TEST: [NPC] Dalir: You’ve now attended 8 meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!
     * REGEX-TEST: [NPC] Dalir: You’ve now attended 4 meetings of the Cult of the Fallen Star! As a reward, here's some Starfall!
     */
    private val starCultPattern by AchievementManager.group.pattern(
        "starcult",
        "\\[NPC] Dalir: You’ve now attended (?<amount>\\d+) meetings of the Cult of the Fallen Star!" +
            " As a reward, here's some Starfall!"
    )

    private const val STAR_CULT_ACHIEVEMENT = "Weekly Cult"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Weekly Cult".asComponent(),
            "Attend 7 cult meetings".asComponent(),
            7f,
        )
        event.register(achievement, STAR_CULT_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        starCultPattern.matchMatcher(event.cleanMessage) {
            if (group("amount").formatInt() >= 7) {
                AchievementManager.completeAchievement(STAR_CULT_ACHIEVEMENT)
            }
        }
    }
}
