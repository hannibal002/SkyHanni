package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun.runDelayed
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object BloodTimer {
    private var bloodOpenTime = SimpleTimeMark.farPast()
    private var bloodOpenTimeServer = ServerTimeMark.farPast()

    private val patternGroup = RepoPattern.group("dungeons.bloodtimer")

    private val bloodOpenMessages by patternGroup.list(
        "open",
        "\\[BOSS] The Watcher: Things feel a little more roomy now, eh\\?",
        "\\[BOSS] The Watcher: Oh\\.\\. hello\\?",
        "\\[BOSS] The Watcher: I'm starting to get tired of seeing you around here\\.\\.\\.",
        "\\[BOSS] The Watcher: You've managed to scratch and claw your way here, eh\\?",
        "\\[BOSS] The Watcher: So you made it this far\\.\\.\\. interesting\\.",
        "\\[BOSS] The Watcher: Ah, we meet again\\.\\.\\.",
        "\\[BOSS] The Watcher: Ah, you've finally arrived\\.",
    )

    private val bloodMoveMessage by patternGroup.pattern(
        "moving",
        "\\[BOSS] The Watcher: Let's see how you can handle this\\.",
    )

    @HandleEvent
    fun onChatReceived(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (bloodOpenMessages.anyMatches(event.cleanMessage)) {
            bloodOpenTime = SimpleTimeMark.now()
            bloodOpenTimeServer = ServerTimeMark.now()
        }

        if (bloodMoveMessage.matches(event.cleanMessage)) {
            val bloodMove = bloodOpenTime.passedSince() + 0.1.seconds
            val bloodMoveTime = bloodOpenTimeServer.passedSince() + 0.1.seconds

            val bloodLag = bloodMove - bloodMoveTime

            ChatUtils.debug("Blood Timer: $bloodMoveTime move time.")

            // Selects move prediction for 4th/5th mob based on how long watcher took to say activation line
            val bloodMovePredictionNumber = selectMoveTime(bloodMoveTime, bloodLag)
            if (bloodMovePredictionNumber == null) {
                ChatUtils.chat("§cInvalid Prediction")
                return
            }
            val bloodMovePrediction = bloodMovePredictionNumber.inPartialSeconds.let { "%.2f".format(it) }
            ChatUtils.chat("§7Move Prediction: §f$bloodMovePrediction Seconds§7.")
            TitleManager.sendTitle("", "§7Move Prediction: §f${bloodMovePrediction}s", 2.5.seconds)
            val delay = bloodMovePredictionNumber - bloodMoveTime - 150.milliseconds
            ChatUtils.debug("Blood Timer: $delay delay.")
            runDelayed(delay) {
                TitleManager.sendTitle("", "§cKill Blood", 1.5.seconds)
            }
        }
    }

    private fun selectMoveTime(bloodMoveTime: Duration, bloodLag: Duration): Duration? {
        return when (bloodMoveTime.inPartialSeconds) {
            in 31.0..34.0 -> bloodLag + 36.seconds
            in 28.0..31.0 -> bloodLag + 33.seconds
            in 25.0..28.0 -> bloodLag + 30.seconds
            in 22.0..25.0 -> bloodLag + 27.seconds
            in 1.0..22.0 -> bloodLag + 24.seconds
            else -> null
        }
    }

    private fun isEnabled() = SkyHanniMod.feature.dungeon.bloodCampTimer && IslandType.CATACOMBS.isCurrent()
}
