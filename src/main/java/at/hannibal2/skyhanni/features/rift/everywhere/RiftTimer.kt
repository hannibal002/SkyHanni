package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftTimer {
    private val config get() = RiftAPI.config.timer
    private var display = emptyList<String>()
    private var maxTime = 0L
    private var latestTime = 0L
    private val changes = mutableMapOf<Long, String>()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        display = emptyList()
        maxTime = 0
        latestTime = 0
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return

        val message = event.message
        for (entry in message.split("     ")) {
            "§(?<color>[a7])(?<time>.*)ф Left.*".toPattern().matchMatcher(entry) {
                val color = group("color")
                if (color == "7") {
                    val currentTime = getTime(group("time"))
                    if (currentTime > maxTime) {
                        maxTime = currentTime
                        update(currentTime)
                    }
                    return
                }
                update(getTime(group("time")))
            }
        }
    }

    private fun getTime(time: String) = TimeUtils.getMillis(time.replace("m", "m "))

    private fun update(currentTime: Long) {
        if (currentTime == latestTime) return
        val diff = (currentTime - latestTime) + 1000
        latestTime = currentTime
        if (latestTime != maxTime) {
            addDiff(diff)
        }

        val currentFormat = TimeUtils.formatDuration(currentTime)
        val percentage = LorenzUtils.formatPercentage(currentTime.toDouble() / maxTime)
        val percentageFormat = if (config.percentage) " §7($percentage)" else ""
        val maxTimeFormat = if (config.maxTime) "§7/§b" + TimeUtils.formatDuration(maxTime) else ""
        val color = if (currentTime <= 60_000) "§c" else if (currentTime <= 60_000 * 5) "§e" else "§b"
        val firstLine = "§eRift Timer: $color$currentFormat$maxTimeFormat$percentageFormat"

        display = buildList {
            add(firstLine)
            changes.keys.removeIf { System.currentTimeMillis() > it + 4_000 }
            for (entry in changes.values) {
                add(entry)
            }
        }
    }

    private fun addDiff(diff: Long) {
        val diffFormat = if (diff > 0) {
            "§a+${TimeUtils.formatDuration(diff)}"
        } else if (diff < 0) {
            "§c-${TimeUtils.formatDuration(-diff)}"
        } else return

        changes[System.currentTimeMillis()] = diffFormat
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockArea == "Mirrorverse") return

        config.timerPosition.renderStrings(display, posLabel = "Rift Timer")
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled
}
