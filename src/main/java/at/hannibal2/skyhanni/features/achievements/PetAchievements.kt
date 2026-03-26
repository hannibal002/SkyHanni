package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object PetAchievements {

    /**
     * REGEX-TEST: Your Pet Score: 435
     */
    private val petScorePattern by AchievementManager.group.pattern(
        "pet-score",
        "Your Pet Score: (?<score>\\d+)",
    )

    private const val PET_SCORE_ACHIEVEMENT = "400 Pet Score"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Oringo Competitor".asComponent(),
            "Oringo thinks true pet collectors have at least 400 Pet Score".asComponent(),
            14f,
        )
        event.register(achievement, PET_SCORE_ACHIEVEMENT)
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!event.inventoryName.startsWith("Pets")) return
        val lore = event.inventoryItems[47]?.getLoreComponent()?.reversed() ?: return
        for (line in lore) {
            petScorePattern.matchMatcher(line) {
                if (group("score").formatInt() >= 400) {
                    AchievementManager.completeAchievement(PET_SCORE_ACHIEVEMENT)
                }
            }
        }
    }
}
