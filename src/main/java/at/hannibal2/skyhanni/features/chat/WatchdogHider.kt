package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ChatLine
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.ReflectionHelper

class WatchdogHider {

    private var inWatchdog = false
    private var startLineComponent: IChatComponent? = null

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel || !SkyHanniMod.feature.chat.watchDog) return

        when (event.message) {
            watchdogStartLine -> {
                startLineComponent = event.chatComponent
            }
            watchdogAnnouncementLine -> {
                ChatManager.retractMessage(startLineComponent, "watchdog")
                startLineComponent = null
                inWatchdog = true
            }
            watchdogEndLine -> {
                event.blockedReason = "watchdog"
                inWatchdog = false
            }
        }

        if (inWatchdog) {
            event.blockedReason = "watchdog"
        }
    }

    companion object {
        private const val watchdogStartLine = "§f"
        private const val watchdogAnnouncementLine = "§4[WATCHDOG ANNOUNCEMENT]"
        private const val watchdogEndLine = "§c"
    }
}


