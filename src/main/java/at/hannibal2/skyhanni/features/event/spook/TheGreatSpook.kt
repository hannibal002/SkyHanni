package at.hannibal2.skyhanni.features.event.spook

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TheGreatSpook {

    // §r§cPrimal Fears§r§7: §r§6§lREADY!!
    private val config get() = SkyHanniMod.feature.event.spook
    private var displayTimer = ""
    private var displayFearStat = ""
    private var displayTimeLeft = ""
    private var notificationSeconds = 0

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (isAllDisabled()) return

        if (isTimerEnabled() || isNotificationEnabled()) displayTimer = checkTabList(" §r§cPrimal Fears§r§7: ")
        if (isFearStatEnabled()) displayFearStat = checkTabList(" §r§5Fear: ")
        if (isTimeLeftEnabled()) displayTimeLeft = checkTabList(" §r§dEnds In§r§7: ")
        if (isNotificationEnabled()) {
            if (displayTimer.endsWith("READY!!")) {
                if (notificationSeconds > 0) {
                    SoundUtils.playBeepSound()
                    notificationSeconds--
                }
            } else if (displayTimer.isNotEmpty()) {
                notificationSeconds = 5
            }
        }
    }

    private fun checkTabList(matchString: String): String {
        return (TabListData.getTabList().find { it.contains(matchString) } ?: "").trim()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (isTimerEnabled()) config.positionTimer.renderString(displayTimer, posLabel = "Primal Fear Timer")
        if (isFearStatEnabled()) config.positionFear.renderString(displayFearStat, posLabel = "Fear Stat Display")
        if (isTimeLeftEnabled()) config.positionTimeLeft.renderString(displayTimeLeft, posLabel = "Time Left Display")
    }

    private fun isTimerEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearTimer

    private fun isNotificationEnabled(): Boolean = LorenzUtils.inSkyBlock && config.primalFearNotification
    private fun isFearStatEnabled(): Boolean = LorenzUtils.inSkyBlock && config.fearStatDisplay
    private fun isTimeLeftEnabled(): Boolean = LorenzUtils.inSkyBlock && config.greatSpookTimeLeft

    private fun isAllDisabled(): Boolean = !isTimeLeftEnabled() && !isTimerEnabled() && !isFearStatEnabled() &&
        !isNotificationEnabled()
}
