package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object HeavyPearlAchievement {

    private val allCollectedPattern by AchievementManager.group.pattern(
        "pearls.allcollected",
        "Find a way to reach the top of the stomach!"
    )

    private val bonusPattern by AchievementManager.group.pattern(
        "pearls.bonus",
        "Your Matriarch Cubs attribute has granted you 1 additional Heavy Pearl!"
    )

    private var currentBonus = 0

    private const val HEAVY_PEARL_ACHIEVEMENT = "Triple Heavy Pearl"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Triple Pearls".asComponent(),
            "Have the Matriarch Cub attribute trigger 3 times".asComponent(),
            30f,
        )
        event.register(achievement, HEAVY_PEARL_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (allCollectedPattern.matches(event.cleanMessage)) {
            currentBonus = 0
        }
        if (bonusPattern.matches(event.cleanMessage)) {
            currentBonus++
        }
        if (currentBonus == 3) {
            AchievementManager.completeAchievement(HEAVY_PEARL_ACHIEVEMENT)
        }
    }
}
