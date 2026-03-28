package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

@SkyHanniModule
object SlayerAchievements {

    /**
     * WRAPPED-REGEX-TEST: "   RNG Meter - 728,269 Stored XP"
     */
    private val rngMeterPattern by AchievementManager.group.pattern(
        "rng-meter",
        "\\s*RNG Meter - (?<xp>[\\d,]+) Stored XP",
    )

    private const val RNG_ACHIEVEMENT_3M = "3m rng xp"
    private const val RNG_ACHIEVEMENT_10M = "10m rng xp"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement3m = Achievement(
            "Desperate Slayer".asComponent(),
            "Get over 3m Stored RNG XP".asComponent(),
            3f,
        )
        val achievement10m = Achievement(
            "Slayer Psycho".asComponent(),
            componentBuilder {
                append("Get over 10m Stored RNG XP! ")
                append("I hope you drop the dye soon!") {
                    withColor(ChatFormatting.DARK_PURPLE)
                }
            },
            10f,
        )
        event.register(achievement3m, RNG_ACHIEVEMENT_3M)
        event.register(achievement10m, RNG_ACHIEVEMENT_10M)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        rngMeterPattern.matchMatcher(event.cleanMessage) {
            val xp = group("xp").formatInt()
            if (xp >= 3_000_000) {
                AchievementManager.completeAchievement(RNG_ACHIEVEMENT_3M)
            }
            if (xp >= 10_000_000) {
                AchievementManager.completeAchievement(RNG_ACHIEVEMENT_10M)
            }
        }
    }
}
