package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object BlizzardAchievement {

    /**
     * REGEX-TEST: BLIZZARD! [MVP+] Throwpo opened a Blizzard in a Bottle, improving everyone's Fishing Stats for the next 10 minutes and causing it to snow!
     */
    private val blizzardPattern by AchievementManager.group.pattern(
        "blizzard",
        "BLIZZARD! (?<name>.*) opened a Blizzard in a Bottle, improving everyone's " +
            "Fishing Stats for the next 10 minutes and causing it to snow!",
    )

    private const val BLIZZARD_ACHIEVEMENT = "Blizzard"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Weather Mastermind".asComponent(),
            "Spawn a blizzard".asComponent(),
            1f,
        )
        event.register(achievement, BLIZZARD_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        blizzardPattern.matchMatcher(event.cleanMessage) {
            if (group("name").cleanPlayerName() == PlayerUtils.getName()) {
                AchievementManager.completeAchievement(BLIZZARD_ACHIEVEMENT)
            }
        }
    }
}
