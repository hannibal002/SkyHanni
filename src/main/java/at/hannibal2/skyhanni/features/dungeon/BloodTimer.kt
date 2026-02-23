package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.BloodMessagesJson
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun.runDelayed
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
    private var bloodOpenLength = ServerTimeMark.farPast()
    private var bloodOpenMessages: List<String> = mutableListOf()
    private var bloodMovingMessages: List<String> = mutableListOf()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<BloodMessagesJson>("dungeons/BloodMessages")
        bloodOpenMessages = data.startMessages
        bloodMovingMessages = data.moveMessages
    }

    @HandleEvent
    fun onChatReceived(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        when (event.cleanMessage) {
            in bloodOpenMessages -> {
                bloodOpenTime = SimpleTimeMark.now()
                bloodOpenLength = ServerTimeMark.now()
            }

            in bloodMovingMessages -> {
                val bloodMove = bloodOpenTime.passedSince() + 0.1.seconds
                val bloodMoveTime = bloodOpenLength.passedSince() + 0.1.seconds

                val bloodLag = bloodMove - bloodMoveTime

                ChatUtils.debug("Blood Timer: $bloodMoveTime move time.")

                // Selects move prediction for 4th/5th mob based on how long watcher took to say activation line
                val bloodMovePredictionNumber: Duration? = when (bloodMoveTime.inPartialSeconds) {
                    in 31.0..34.0 -> bloodLag + 36.seconds
                    in 28.0..31.0 -> bloodLag + 33.seconds
                    in 25.0..28.0 -> bloodLag + 30.seconds
                    in 22.0..25.0 -> bloodLag + 27.seconds
                    in 1.0..22.0 -> bloodLag + 24.seconds
                    else -> null
                }
                val bloodMovePrediction = bloodMovePredictionNumber?.inPartialSeconds?.let { "%.2f".format(it) }

                bloodMovePrediction?.let {
                    ChatUtils.chat("§7Move Prediction: §f$it Seconds§7.")
                    TitleManager.sendTitle("", "§7Move Prediction: §f${it}s", 2.5.seconds)
                    val delay = bloodMovePredictionNumber - bloodMoveTime - 150.milliseconds
                    ChatUtils.debug("Blood Timer: $delay delay.")
                    runDelayed(delay) {
                        TitleManager.sendTitle("", "§cKill Blood", 1.5.seconds)
                    }
                } ?: run {
                    ChatUtils.chat("§cInvalid Prediction")
                }
            }
        }
    }

    fun isEnabled() = SkyHanniMod.feature.dungeon.bloodCampTimer && IslandType.CATACOMBS.isCurrent()

}
