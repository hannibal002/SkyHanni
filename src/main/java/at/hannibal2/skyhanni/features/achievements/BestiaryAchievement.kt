package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessaryOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object BestiaryAchievement {

    /**
     * REGEX-TEST: Bestiary Milestone CCCXX
     * REGEX-TEST: Bestiary Milestone 320
     */
    private val bestiaryPattern by AchievementManager.group.pattern(
        "bestiary",
        "Bestiary Milestone (?<milestone>.*)"
    )

    private const val BESTIARY_ACHIEVEMENT = "bestiary"
    val bestiaryDetector = InventoryDetector(checkInventoryName = { it == "Bestiary" })

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Insistent Murderer".asComponent(),
            "Unlock Bestiary Milestones".asComponent(),
            50f,
            false,
            listOf(100, 200, 300),
        )
        event.register(achievement, BESTIARY_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!bestiaryDetector.isInside()) return
        val milestoneSlot = 51
        val name = event.inventoryItems[milestoneSlot]?.hoverName ?: return
        bestiaryPattern.matchMatcher(name) {
            val achievement = AchievementManager.getAchievement(BESTIARY_ACHIEVEMENT)
            val milestone = group("milestone").romanToDecimalIfNecessaryOrNull() ?: return
            if (milestone > achievement.data.progress) {
                AchievementManager.updateTieredAchievement(BESTIARY_ACHIEVEMENT, milestone)
            }
        }
    }
}
