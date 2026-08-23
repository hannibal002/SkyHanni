package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.matches

@SkyHanniModule
object SowdustAchievement {

    /**
     * WRAPPED-REGEX-TEST: " - 250,000,000 Sowdust"
     */
    private val maxSowdustPattern by AchievementManager.group.pattern(
        "sowdust",
        " - 250,000,000 Sowdust",
    )

    /**
     * REGEX-TEST: Manage Chips
     */
    private val manageChipsInventoryPattern by AchievementManager.group.pattern(
        "manage-chips-inventory",
        "Manage Chips",
    )

    private const val SOWDUST_ACHIEVEMENT = "sowdust"
    val chipsDetector = InventoryDetector { manageChipsInventoryPattern }

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            name = "Funky Tasting Chips",
            description = "Put maximum seasoning on your chips",
            userLuckAmount = 50f,
        )
        event.register(achievement, SOWDUST_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (AchievementManager.isCompleted(SOWDUST_ACHIEVEMENT)) return
        if (!chipsDetector.isInside()) return
        val milestoneSlot = 53
        val lore = event.inventoryItems[milestoneSlot]?.getLoreComponent() ?: return
        for (line in lore) {
            if (maxSowdustPattern.matches(line)) {
                AchievementManager.completeAchievement(SOWDUST_ACHIEVEMENT)
            }
        }
    }
}
