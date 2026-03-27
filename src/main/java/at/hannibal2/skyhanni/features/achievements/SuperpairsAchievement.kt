package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentsAddonsHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object SuperpairsAchievement {

    private const val CHRONOMATRON_ACHIEVEMENT = "20 Chronomatron"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "\"Memorisation\" Professional".asComponent(),
            "Wow you have such good memory".asComponent(),
            2f,
        )
        event.register(achievement, CHRONOMATRON_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryFullyOpened(event: InventoryUpdatedEvent) {
        if (!ExperimentationTableApi.inChronomatron) return
        if (ExperimentsAddonsHelper.currentChronomatronRound >= 20) {
            AchievementManager.completeAchievement(CHRONOMATRON_ACHIEVEMENT)
        }
    }
}
