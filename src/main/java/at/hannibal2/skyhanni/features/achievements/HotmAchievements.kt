package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object HotmAchievements {

    private const val PICKAXE_ABILITY_ACHIEVEMENT = "ability master"
    val hotmDetector = InventoryDetector(checkInventoryName = { it == "Heart of the Mountain" })

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Have 4 Pickaxe Abilities unlocked".asComponent(),
            "Jack of all Abilities, master of none".asComponent(),
            4f,
            true,
        )
        event.register(achievement, PICKAXE_ABILITY_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!hotmDetector.isInside()) return
        var count = 0
        for (ability in HotmData.abilities) {
            if (ability.isUnlocked) count++
        }
        if (count >= 4) {
            AchievementManager.completeAchievement(PICKAXE_ABILITY_ACHIEVEMENT)
        }
    }
}
