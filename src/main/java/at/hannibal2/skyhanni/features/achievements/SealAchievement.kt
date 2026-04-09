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
object SealAchievement {

    /**
     * REGEX-TEST: INSANE! You kept the Bouncy Beach Ball in the air for 187 bounces and earned 20 Fishy Treats!
     */
    private val sealBouncePattern by AchievementManager.group.pattern(
        "seal-bounce",
        "INSANE! You kept the Bouncy Beach Ball in the air for (?<bounces>\\d+) bounces and earned \\d+ Fishy Treats!",
    )

    private const val SEAL_BOUNCE_ACHIEVEMENT = "Seal Bounces"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Seal Lookalike".asComponent(),
            "Bounce a ball 100 times".asComponent(),
            1f,
        )
        event.register(achievement, SEAL_BOUNCE_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        sealBouncePattern.matchMatcher(event.cleanMessage) {
            if (group("bounces").formatInt() >= 100) {
                AchievementManager.completeAchievement(SEAL_BOUNCE_ACHIEVEMENT)
            }
        }
    }
}
