package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelUpEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object SkillAchievements {

    private const val SKILL_ACHIEVEMENT = "Level 100 Skill"
    private const val ROCK_ACHIEVEMENT = "Mythic Rock"
    private const val DOLPHIN_ACHIEVEMENT = "Mythic Dolphin"
    val skillDetector = InventoryDetector(checkInventoryName = { it == "Your Skills" })
    val petSkillDetector = InventoryDetector(pattern = "(Fishing|Mining) Skill".toPattern())

    /**
     * REGEX-TEST: Ores mined: 2,449,790
     */
    private val oresPattern by AchievementManager.group.pattern(
        "ores",
        "Ores mined: (?<amount>[\\d,]+)",
    )

    /**
     * REGEX-TEST: Sea Creatures killed: 63,641
     */
    private val seaCreaturesPattern by AchievementManager.group.pattern(
        "sea-creatures",
        "Sea Creatures killed: (?<amount>[\\d,]+)",
    )

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val skill100Achievement = Achievement(
            "Expert Skiller".asComponent(),
            "Get a skill to level 70/80/90/100".asComponent(),
            50f,
            false,
            listOf(70, 80, 90, 100),
        )
        val rockAchievement = Achievement(
            "Where's my Mythic Rock Pet".asComponent(),
            "Mine 1 million Ores".asComponent(),
            10f,
        )
        val dolphinAchievement = Achievement(
            "Mythical Dolphin Pull".asComponent(),
            "Kill 50,000 Sea Creatures".asComponent(),
            10f,
        )
        event.register(skill100Achievement, SKILL_ACHIEVEMENT)
        event.register(rockAchievement, ROCK_ACHIEVEMENT)
        event.register(dolphinAchievement, DOLPHIN_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSkillOverflowLevel(event: SkillOverflowLevelUpEvent) {
        val achievement = AchievementManager.getAchievement(SKILL_ACHIEVEMENT)
        if (achievement.data.progress >= event.newLevel) return
        AchievementManager.updateTieredAchievement(SKILL_ACHIEVEMENT, event.newLevel)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val petSlot = 51
        val lore = event.inventoryItems[petSlot]?.getLoreComponent()
        if (skillDetector.isInside()) {
            DelayedRun.runNextTick {
                val storage = ProfileStorageData.profileSpecific?.skillData ?: return@runNextTick
                var highestSkill = 0
                for ((_, info) in storage) {
                    if (info.overflowLevel > highestSkill) highestSkill = info.overflowLevel
                }
                val achievement = AchievementManager.getAchievement(SKILL_ACHIEVEMENT)
                if (achievement.data.progress != highestSkill) {
                    AchievementManager.updateTieredAchievement(SKILL_ACHIEVEMENT, highestSkill)
                }
            }
        } else if (petSkillDetector.isInside() && lore != null) {
            for (line in lore) {
                oresPattern.matchMatcher(line) {
                    if (group("amount").formatInt() > 1_000_000) {
                        AchievementManager.completeAchievement(ROCK_ACHIEVEMENT)
                    }
                }
                seaCreaturesPattern.matchMatcher(line) {
                    if (group("amount").formatInt() > 50_000) {
                        AchievementManager.completeAchievement(DOLPHIN_ACHIEVEMENT)
                    }
                }
            }
        }
    }

}
