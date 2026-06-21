package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandType.Companion.isInAnyIsland
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

@SkyHanniModule
object XpAchievement {

    private const val XP_ACHIEVEMENT = "High Level Gamer"
    private val ignoredAreas = setOf(IslandType.THE_RIFT, IslandType.CATACOMBS)

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "High Level Gamer".asComponent(),
            "Have 1000 XP Levels".asComponent(),
            25f,
        )
        event.register(achievement, XP_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (AchievementManager.isCompleted(XP_ACHIEVEMENT)) return
        if (ignoredAreas.isInAnyIsland()) return
        if (MinecraftCompat.localPlayer.experienceLevel == 1000) {
            AchievementManager.completeAchievement(XP_ACHIEVEMENT)
        }
    }
}
