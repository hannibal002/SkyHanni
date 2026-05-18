package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureFishEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object JawbusAchievements {

    private const val JAWBUS_ACHIEVEMENT = "Double Jawbus"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Lord Almighty".asComponent(),
            "Double Hook a Lord Jawbus".asComponent(),
            20f,
        )
        event.register(achievement, JAWBUS_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSeaCreature(event: SeaCreatureFishEvent) {
        if (!event.doubleHook) return
        if (event.seaCreature.name != "Lord Jawbus") return
        AchievementManager.completeAchievement(JAWBUS_ACHIEVEMENT)
    }
}
