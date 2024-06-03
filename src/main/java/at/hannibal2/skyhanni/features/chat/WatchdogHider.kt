package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object WatchdogHider {

    private var inWatchdog = false
    private var blockedLines = 0
    private var startLineComponent: IChatComponent? = null

    private const val WATCHDOG_START_LINE = "§f"
    private const val WATCHDOG_ANNOUNCEMENT_LINE = "§4[WATCHDOG ANNOUNCEMENT]"
    private const val WATCHDOG_END_LINE = "§c"

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel || !SkyHanniMod.feature.chat.filterType.watchDog) return

        when (event.message) {
            WATCHDOG_START_LINE -> {
                startLineComponent = event.chatComponent
                blockedLines = 0
            }

            WATCHDOG_ANNOUNCEMENT_LINE -> {
                ChatManager.retractMessage(startLineComponent, "watchdog")
                startLineComponent = null
                inWatchdog = true
            }

            WATCHDOG_END_LINE -> {
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
}


