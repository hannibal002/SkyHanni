package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object DianaAchievements {

    private const val INQ_ACHIEVEMENT = "Death Via Inq"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "That was that book...".asComponent(),
            "Die to an Inquisitor".asComponent(),
            5f,
        )
        event.register(achievement, INQ_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerDeath(event: PlayerDeathEvent.Allow) {
        if (!event.isSelf) return
        if (!event.reason.contains("Minos Inquisitor")) return
        AchievementManager.completeAchievement(INQ_ACHIEVEMENT)
    }
}
