package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object VanquisherAchievement {

    /**
     * REGEX-TEST: A Vanquisher is spawning nearby!
     */
    private val vanquisherSpawnPattern by AchievementManager.group.pattern(
        "vanquisher",
        "A Vanquisher is spawning nearby!",
    )

    private const val VANQUISHER_ACHIEVEMENT = "Double Trouble"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Vanquisher Double Trouble".asComponent(),
            "Spawn two of them within one second".asComponent(),
            25f,
            true,
        )
        event.register(achievement, VANQUISHER_ACHIEVEMENT)
    }

    private var lastVanquisher = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!vanquisherSpawnPattern.matches(event.cleanMessage)) return
        if (lastVanquisher.passedSince() < 1.seconds) {
            AchievementManager.completeAchievement(VANQUISHER_ACHIEVEMENT)
        } else {
            lastVanquisher = SimpleTimeMark.now()
        }
    }
}
