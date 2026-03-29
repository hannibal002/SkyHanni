package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@Suppress("MaxLineLength")
@SkyHanniModule
object DungeonsAchievements {

    /**
     * REGEX-TEST: RNG METER! Reselected the Enchanted Book (Rejuvenate I) for Catacombs - Floor 1! CLICK HERE to select a new drop!
     */
    private val woodRngPattern by AchievementManager.group.pattern(
        "wood-rng",
        "RNG METER! Reselected the Enchanted Book \\((?:Rejuvenate I|Infinite Quiver VI|Feather Falling VI|Bank I|Ultimate Jerry I)\\) for Catacombs - Floor 1! CLICK HERE to select a new drop!",
    )

    private const val WOOD_ACHIEVEMENT = "small rng drop"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Use RNG Meter to get a Book from F1 Wood Chest".asComponent(),
            "Even the smallest rng drop can be chosen.".asComponent(),
            10f,
            true,
        )
        event.register(achievement, WOOD_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (woodRngPattern.matches(event.cleanMessage)) {
            AchievementManager.completeAchievement(WOOD_ACHIEVEMENT)
        }
    }
}
