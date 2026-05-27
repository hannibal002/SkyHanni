package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.data.ChocolateAmount
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object HoppityAchievements {

    /**
     * REGEX-TEST: Rabbits Found: 95.7%
     */
    private val rabbitsFoundPattern by AchievementManager.group.pattern(
        "rabbits-found",
        "Rabbits Found: (?<percent>[\\d.]+)%"
    )

    private const val RABBITS_FOUND_ACHIEVEMENT = "rabbit collector"
    private const val CHOCOLATE_FULL_ACHIEVEMENT = "60b chocolate"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val rabbitAchievement = Achievement(
            "Hoppity's Assistant".asComponent(),
            "Bring home all of Hoppity's Rabbits".asComponent(),
            200f,
            false,
            listOf(80, 90, 100),
        )
        val chocolateAchievement = Achievement(
            "Waste of chooclate".asComponent(),
            "Have 60 Billion Chocolate sitting in your Chocolate Factory".asComponent(),
            5f,
        )
        event.register(rabbitAchievement, RABBITS_FOUND_ACHIEVEMENT)
        event.register(chocolateAchievement, CHOCOLATE_FULL_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!CFApi.mainInventory.isInside()) return
        if (ChocolateAmount.CURRENT.chocolate() == 60_000_000_000L) {
            AchievementManager.completeAchievement(CHOCOLATE_FULL_ACHIEVEMENT)
        }
        val milestoneSlot = 50
        val lore = event.inventoryItems[milestoneSlot]?.getLoreComponent() ?: return
        for (line in lore) {
            rabbitsFoundPattern.matchMatcher(line) {
                val achievement = AchievementManager.getAchievement(RABBITS_FOUND_ACHIEVEMENT)
                val percent = group("percent").formatInt()
                if (percent > achievement.data.progress) {
                    AchievementManager.updateTieredAchievement(RABBITS_FOUND_ACHIEVEMENT, percent)
                }
            }
        }
    }
}
