package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object EggAchievement {

    private val eggPatternPattern by AchievementManager.group.pattern(
        "laid-egg",
        "You laid an egg!",
    )

    private const val EGG_ACHIEVEMENT = "egg layer"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            name = "Lay 25 Eggs".asComponent(),
            description = TextHelper.createAtlasSprite("item/egg", "items", "minecraft"),
            userLuckAmount = 1f,
            secret = true,
            tiers = listOf(25),
        )
        event.register(achievement, EGG_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (eggPatternPattern.matches(event.cleanMessage)) {
            val achievement = AchievementManager.getAchievement(EGG_ACHIEVEMENT)
            AchievementManager.updateTieredAchievement(EGG_ACHIEVEMENT, achievement.data.progress + 1)
        }
    }
}
