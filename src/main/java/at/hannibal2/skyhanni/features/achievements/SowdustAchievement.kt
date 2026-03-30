package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object SowdustAchievement {

    private val maxSowdustPattern by AchievementManager.group.pattern(
        "sowdust",
        " - 250,000,000 Sowdust"
    )

    private const val SOWDUST_ACHIEVEMENT = "sowdust"
    val chipsDetector = InventoryDetector(checkInventoryName = { it == "Manage Chips" })

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Funky Tasting Chips".asComponent(),
            "Put maximum seasoning on your chips".asComponent(),
            50f,
        )
        event.register(achievement, SOWDUST_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
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
