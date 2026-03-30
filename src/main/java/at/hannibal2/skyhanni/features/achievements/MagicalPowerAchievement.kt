package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object MagicalPowerAchievement {

    private const val MP_ACHIEVEMENT = "Magic Sigma"
    val accessoryBagDetector = InventoryDetector(
        pattern = "Accessory Bag.*".toPattern(),
    )

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Magical Sigma".asComponent(),
            "Pro tip: Magical Power gives you good stats".asComponent(),
            200f,
            false,
            listOf(500, 1000, 1500, 1800, 2000),
        )
        event.register(achievement, MP_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened() {
        if (!accessoryBagDetector.isInside()) return
        val mp = ProfileStorageData.profileSpecific?.maxwell?.magicalPower ?: return
        val achievement = AchievementManager.getAchievement(MP_ACHIEVEMENT)
        if (mp > achievement.data.progress) {
            AchievementManager.updateTieredAchievement(MP_ACHIEVEMENT, mp)
        }
    }
}
