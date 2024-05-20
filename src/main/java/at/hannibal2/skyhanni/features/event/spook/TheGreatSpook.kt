package at.hannibal2.skyhanni.features.event.spook

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class TheGreatSpook {

    // §r§cPrimal Fears§r§7: §r§6§lREADY!!
    private val config get() = SkyHanniMod.feature.event.spook
    private var displayTimer = ""
    private var displayFearStat = ""
    private var displayTimeLeft = ""
    private var notificationSeconds = 0

    private val patternGroup = RepoPattern.group("thegreatspook")
    private val primalPattern by patternGroup.pattern(
        "primal",
        " §r§cPrimal Fears§r§7: "
    )
    private val fearPattern by patternGroup.pattern(
        "fear",
        " §r§5Fear: "
    )
    private val endPattern by patternGroup.pattern(
        "end",
        " §r§dEnds In§r§7: "
    )

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (isAllDisabled()) return
        if (!event.repeatSeconds(1)) return

        if (isTimerEnabled() || isNotificationEnabled()) displayTimer = checkTabList(primalPattern)
        if (isFearStatEnabled()) displayFearStat = checkTabList(fearPattern)
        if (isTimeLeftEnabled()) displayTimeLeft = checkTabList(endPattern)
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

    private fun checkTabList(pattern: Pattern): String {
        return (TabListData.getTabList().find { pattern.matcher(it).find() } ?: "").trim()
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
