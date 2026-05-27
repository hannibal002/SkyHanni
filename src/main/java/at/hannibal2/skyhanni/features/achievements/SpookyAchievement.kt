package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.componentBuilder

@SkyHanniModule
object SpookyAchievement {

    /**
     * WRAPPED-REGEX-TEST: "                   Your Candy: 4,036 (Position #342)"
     */
    private val candyPattern by AchievementManager.group.pattern(
        "spooky-candy",
        " +Your Candy: (?<candy>[\\d,]+) \\(Position #[\\d,]+\\)"
    )

    private const val SPOOKY_ACHIEVEMENT = "Spooky Double Points"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val netheriteIngot = TextHelper.createAtlasSprite("item/netherite_ingot", "items", "minecraft")
        val achievement = Achievement(
            "Netherite Spooky Bracket".asComponent(),
            componentBuilder {
                append(netheriteIngot)
                append(" Get 10,000 Candy Score in a Spooky Festival ")
                append(netheriteIngot)
            },
            15f,
        )
        event.register(achievement, SPOOKY_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        candyPattern.matchMatcher(event.cleanMessage) {
            if (group("candy").formatInt() >= 10_000) {
                AchievementManager.completeAchievement(SPOOKY_ACHIEVEMENT)
            }
        }
    }
}
