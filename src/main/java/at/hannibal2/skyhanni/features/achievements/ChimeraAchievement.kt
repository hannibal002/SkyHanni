package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object ChimeraAchievement {
    private const val CHIMERA_ACHIEVEMENT = "chim v"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Pet Symbiosis".asComponent(),
            "Make your weapon gain all the stats of your pet".asComponent(),
            25f,
        )
        event.register(achievement, CHIMERA_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: EntityClickEvent) {
        val enchantments = event.itemInHand?.getHypixelEnchantments() ?: return
        if (enchantments["ultimate_chimera"] == 5) {
            AchievementManager.completeAchievement(CHIMERA_ACHIEVEMENT)
        }
    }
}
