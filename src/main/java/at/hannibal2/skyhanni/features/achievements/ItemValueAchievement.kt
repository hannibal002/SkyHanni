package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object ItemValueAchievement {

    private const val ITEM_VALUE_ACHIEVEMENT = "Hefty Items"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Hefty Item".asComponent(),
            "Swing an item worth more than 500mil".asComponent(),
            50f,
        )
        event.register(achievement, ITEM_VALUE_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (AchievementManager.isCompleted(ITEM_VALUE_ACHIEVEMENT)) return
        val stack = event.newStack
        val value = EstimatedItemValueCalculator.getTotalPrice(stack, ignoreBasePrice = true) ?: return
        if (value > 500_000_000) {
            AchievementManager.completeAchievement(ITEM_VALUE_ACHIEVEMENT)
        }
    }
}
