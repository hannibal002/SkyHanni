package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object JerrySeedAchievement {

    private val JERRYSEED = "UNRIPE_JERRYSEED".toInternalName()
    private const val JERRYSEED_ACHIEVEMENT = "OFA Jerryseed"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Jerry".asComponent(),
            "Make an Unripe Jerryseed do 5x damage".asComponent(),
            -1f,
        )
        event.register(achievement, JERRYSEED_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: OwnInventoryItemUpdateEvent) {
        val stack = event.itemStack
        if (stack.getInternalNameOrNull() != JERRYSEED) return
        val enchantments = stack.getHypixelEnchantments() ?: return
        if (enchantments.contains("ultimate_one_for_all")) {
            AchievementManager.completeAchievement(JERRYSEED_ACHIEVEMENT)
        }
    }
}
