package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WatchdogHider {

    private var inWatchdog = false
    private var blockedLines = 0
    private var startLineComponent: IChatComponent? = null

    private val patternGroup = RepoPattern.group("hider.watchdog")
    private val startLinePattern by patternGroup.pattern(
        "startline",
        "§f"
    )
    private val middleLinePattern by patternGroup.pattern(
        "middleline",
        "§4\\[WATCHDOG ANNOUNCEMENT]"
    )
    private val endLinePattern by patternGroup.pattern(
        "endline",
        "§c"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel || !SkyHanniMod.feature.chat.filterType.watchDog) return

        when {
            startLinePattern.matches(event.message) -> {
                startLineComponent = event.chatComponent
                blockedLines = 0
            }

            middleLinePattern.matches(event.message) -> {
                ChatManager.retractMessage(startLineComponent, "watchdog")
                startLineComponent = null
                inWatchdog = true
            }

            endLinePattern.matches(event.message) -> {
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


