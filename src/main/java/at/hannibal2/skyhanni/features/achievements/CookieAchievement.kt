package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import kotlin.time.Duration.Companion.days

@SkyHanniModule
object CookieAchievement {

    private const val COOKIE_ACHIEVEMENT = "cookie fan"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Cookie Monster Super Fan".asComponent(),
            "Get 6 months of Cookie Buff".asComponent(),
            6f,
        )
        event.register(achievement, COOKIE_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(100)) return
        val time = BitsApi.cookieBuffTime ?: return
        if (time.timeUntil() > 180.days) {
            AchievementManager.completeAchievement(COOKIE_ACHIEVEMENT)
        }
    }
}
