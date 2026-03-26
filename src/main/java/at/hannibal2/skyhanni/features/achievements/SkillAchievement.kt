package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.SkillOverflowLevelUpEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object SkillAchievement {

    private const val SKILL_ACHIEVEMENT = "Level 100 Skill"
    val skillDetector = InventoryDetector(checkInventoryName = { it == "Your Skills" })

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Expert Skiller".asComponent(),
            "Get a skill to level 70/80/90/100".asComponent(),
            50f,
            false,
            listOf(70, 80, 90, 100),
        )
        event.register(achievement, SKILL_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkillOverflowLevel(event: SkillOverflowLevelUpEvent) {
        val achievement = AchievementManager.getAchievement(SKILL_ACHIEVEMENT)
        if (achievement.data.progress >= event.newLevel) return
        AchievementManager.updateTieredAchievement(SKILL_ACHIEVEMENT, event.newLevel)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened() {
        if (!skillDetector.isInside()) return
        val storage = ProfileStorageData.profileSpecific?.skillData ?: return
        var highestSkill = 0
        for ((_, info) in storage) {
            if (info.overflowLevel > highestSkill) highestSkill = info.overflowLevel
        }
        val achievement = AchievementManager.getAchievement(SKILL_ACHIEVEMENT)
        if (highestSkill > achievement.data.progress) {
            AchievementManager.updateTieredAchievement(SKILL_ACHIEVEMENT, highestSkill)
        }
    }

}
