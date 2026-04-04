package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper

@SkyHanniModule
object PleadAchievement {

    private val pleadComponent = TextHelper.createAtlasSprite("plead")
    private const val PLEAD_ACHIEVEMENT = "Plead"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            pleadComponent,
            pleadComponent,
            secret = true
        )
        event.register(achievement, PLEAD_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: PlayerAllChatEvent.Allow) {
        if (!event.author.contains(PlayerUtils.getName())) return
        if (!event.cleanMessage.contains("plead")) return
        AchievementManager.completeAchievement(PLEAD_ACHIEVEMENT)

    }
}
