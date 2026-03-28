package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object VincentAchievement {

    /**
     * REGEX-TEST: Vincent accepts your rose and is delighted to visit your Garden soon.
     */
    private val vincentPattern by AchievementManager.group.pattern(
        "vincent",
        "Vincent accepts your rose and is delighted to visit your Garden soon.",
    )

    private const val VINCENT_ACHIEVEMENT = "Strawberry Collector"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Strawberry Collector".asComponent(),
            "Attempt to turn a rose into a strawberry".asComponent(),
            10f,
            false,
            listOf(10)
        )
        event.register(achievement, VINCENT_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!vincentPattern.matches(event.cleanMessage)) return
        val achievement = AchievementManager.getAchievement(VINCENT_ACHIEVEMENT)
        AchievementManager.updateTieredAchievement(VINCENT_ACHIEVEMENT, achievement.data.progress + 1)
    }
}
