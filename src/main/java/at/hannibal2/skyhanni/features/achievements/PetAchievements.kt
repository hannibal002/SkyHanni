package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import kotlin.collections.contains

@SkyHanniModule
object PetAchievements {

    /**
     * REGEX-TEST: Your Pet Score: 435
     */
    private val petScorePattern by AchievementManager.group.pattern(
        "pet-score",
        "Your Pet Score: (?<score>\\d+)",
    )

    private const val PET_SCORE_ACHIEVEMENT = "400 Pet Score"
    private const val PET_EXP_ACHIEVEMENT = "Level 300 Pet"
    private const val TURTLE_ACHIEVEMENT = "Unmoveable"
    private val TURTLE = "TURTLE;4".toInternalName()
    private val shelmets = listOf("DWARF_TURTLE_SHELMET".toInternalName(), "HEPHAESTUS_SHELMET".toInternalName())
    private val SLIME_HAT = "SLIME_HAT".toInternalName()

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val petScoreAchievement = Achievement(
            "Oringo Competitor".asComponent(),
            "Oringo thinks true pet collectors have at least 400 Pet Score".asComponent(),
            14f,
        )
        val petLevelAchievement = Achievement(
            "Over Achiever".asComponent(),
            "Have a Pet with enough XP to get Level 300".asComponent(),
            30f,
        )
        val turtleAchievement = Achievement(
            "Have every anti-knockback item equipped".asComponent(),
            "Be ultra immune to knockback".asComponent(),
            25f,
            true,
        )
        event.register(petScoreAchievement, PET_SCORE_ACHIEVEMENT)
        event.register(petLevelAchievement, PET_EXP_ACHIEVEMENT)
        event.register(turtleAchievement, TURTLE_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (event.repeatSeconds(10)) return
        val pets = ProfileStorageData.petProfiles?.pets ?: return
        if (InventoryUtils.getHelmet()?.getInternalNameOrNull() != SLIME_HAT) return
        for (pet in pets) {
            if (pet.fauxInternalName == TURTLE && pet.heldItemInternalName in shelmets) {
                AchievementManager.completeAchievement(TURTLE_ACHIEVEMENT)
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!event.inventoryName.startsWith("Pets")) return
        val pets = ProfileStorageData.petProfiles?.pets ?: return
        for (pet in pets) {
            val xp = pet.exp ?: 0.0
            if (xp >= 398_925_385) {
                AchievementManager.completeAchievement(PET_EXP_ACHIEVEMENT)
                break
            }
        }
        val lore = event.inventoryItems[47]?.getLoreComponent()?.reversed() ?: return
        for (line in lore) {
            petScorePattern.matchMatcher(line) {
                if (group("score").formatInt() >= 400) {
                    AchievementManager.completeAchievement(PET_SCORE_ACHIEVEMENT)
                }
            }
        }
    }
}
