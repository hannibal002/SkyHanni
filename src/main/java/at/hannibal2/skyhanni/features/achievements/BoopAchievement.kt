package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

@SkyHanniModule
object BoopAchievement {

    /**
     * REGEX-TEST: To [MVP+] Bloxigus: Boop!
     * REGEX-TEST: To qtLuna: Boop!
     */
    private val boopPattern by AchievementManager.group.pattern(
        "boop",
        "To .*: Boop!"
    )

    private const val BOOP_ACHIEVEMENT = "Social Butterfly"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Social Butterfly".asComponent(),
            componentBuilder {
                append("Annoy 10 people with ")
                append("BOOP!") {
                    withColor(ChatFormatting.LIGHT_PURPLE)
                    bold = true
                }
            },
            userLuckAmount = 1f,
            secret = false,
            tiers = listOf(10),
        )
        event.register(achievement, BOOP_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (boopPattern.matches(event.cleanMessage)) {
            val achievement = AchievementManager.getAchievement(BOOP_ACHIEVEMENT)
            AchievementManager.updateTieredAchievement(BOOP_ACHIEVEMENT, achievement.data.progress + 1)
        }
    }
}
