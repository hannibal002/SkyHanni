package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.hours

@SkyHanniModule
object UbikReminder {

    private val config get() = RiftApi.config

    private var isTimerRunning = false
    private val patternGroup = RepoPattern.group("rift.ubik")

    /**
     * REGEX-TEST: ROUND 2 (FINAL):
     */
    private val ubikRoundPattern by patternGroup.pattern(
        "reminder",
        "ROUND [1-9] \\(FINAL\\):",
    )

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.ubikReminder) return
        val message = event.message
        if (ubikRoundPattern.matches(message) && !isTimerRunning) {
            startTimer()
        }
    }

    private fun startTimer() {
        isTimerRunning = true

        DelayedRun.runDelayed(2.hours) { // 2 hours as a Duration
            if (config.ubikReminder) {
                ChatUtils.chat("§aUbik's cube is ready in the rift!")
            }
            isTimerRunning = false
        }
    }
}
