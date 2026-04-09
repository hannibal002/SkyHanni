package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object RecipeAchievement {

    /**
     * REGEX-TEST: Recipe Book Unlocked: 98.6%
     */
    private val recipeBookPattern by AchievementManager.group.pattern(
        "recipe-book",
        "Recipe Book Unlocked: (?<percent>[\\d.]+)%"
    )

    private const val RECIPE_ACHIEVEMENT = "Recipe Unlocker"
    val sbMenuDetector = InventoryDetector(checkInventoryName = { it == "SkyBlock Menu" })

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Recipe Fanatic".asComponent(),
            "Unlock all the Recipes".asComponent(),
            50f,
            false,
            listOf(50, 70, 90, 100),
        )
        event.register(achievement, RECIPE_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!sbMenuDetector.isInside()) return
        val recipeSlot = 21
        val lore = event.inventoryItems[recipeSlot]?.getLoreComponent() ?: return
        for (line in lore) {
            recipeBookPattern.matchMatcher(line) {
                val achievement = AchievementManager.getAchievement(RECIPE_ACHIEVEMENT)
                val percent = group("percent").formatInt()
                if (percent > achievement.data.progress) {
                    AchievementManager.updateTieredAchievement(RECIPE_ACHIEVEMENT, percent)
                }
            }
        }
    }
}
