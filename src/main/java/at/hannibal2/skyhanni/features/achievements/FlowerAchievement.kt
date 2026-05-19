package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object FlowerAchievement {

    private const val ROMERO_ACHIEVEMENT = "Romeros Hotbar"
    private val SKYBLOCK_MENU = "SKYBLOCK_MENU".toInternalName()

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Fill your hotbar with flowers".asComponent(),
            "Have your hotbar look like Romero's".asComponent(),
            userLuckAmount = 30f,
            secret = true,
        )
        event.register(achievement, ROMERO_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (AchievementManager.isCompleted(ROMERO_ACHIEVEMENT)) return
        val flowerSack = SackApi.sacks["Flower"] ?: return
        var counter = 0
        for (stack in InventoryUtils.getItemsInHotbar()) {
            val internalName = stack.getInternalNameOrNull()
            if (internalName !in flowerSack && internalName != SKYBLOCK_MENU) return
            counter++
        }
        if (counter != 9) return
        AchievementManager.completeAchievement(ROMERO_ACHIEVEMENT)
    }
}
