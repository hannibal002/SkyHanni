package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object WatchdogHider {

    private var inWatchdog = false
    private var blockedLines = 0
    private var startLineComponent: IChatComponent? = null

    private val patternGroup = RepoPattern.group("watchdoghider")
    private val watchdogMessagePattern by patternGroup.pattern(
        "message",
        "§4\\[WATCHDOG ANNOUNCEMENT]"
    )

    private const val WATCHDOG_START_LINE = "§f"
    private const val WATCHDOG_END_LINE = "§c"

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel || !SkyHanniMod.feature.chat.filterType.watchDog) return

        val message = event.message

        when {
            message == WATCHDOG_START_LINE -> {
                startLineComponent = event.chatComponent
                blockedLines = 0
            }

            watchdogMessagePattern.matches(message) -> {
                ChatManager.retractMessage(startLineComponent, "watchdog")
                startLineComponent = null
                inWatchdog = true
            }

            message == WATCHDOG_END_LINE -> {
                event.blockedReason = "watchdog"
                inWatchdog = false
            }
        }

        if (inWatchdog) {
            event.blockedReason = "watchdog"
            blockedLines++
            if (blockedLines > 10) {
                blockedLines = 0
                inWatchdog = false
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "chat.watchDog", "chat.filterType.watchDog")
    }
}


