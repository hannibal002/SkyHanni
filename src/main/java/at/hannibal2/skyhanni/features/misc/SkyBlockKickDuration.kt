package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SkyBlockKickDuration {
    private val config get() = SkyHanniMod.feature.misc.kickDuration

    var kickMessage = false
    var showTime = false
    var lastKickTime = SimpleTimeMark.farPast()
    var hasWarned = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (event.message == "§cYou were kicked while joining that server!") {

            if (LorenzUtils.onHypixel && !LorenzUtils.inSkyBlock) {
                kickMessage = false
                showTime = true
                lastKickTime = SimpleTimeMark.farPast()
            } else {
                kickMessage = true
            }
        }

        if (event.message == "§cThere was a problem joining SkyBlock, try again in a moment!") {
            kickMessage = false
            showTime = true
            lastKickTime = SimpleTimeMark.farPast()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!isEnabled()) return
        if (kickMessage) {
            kickMessage = false
            showTime = true
            lastKickTime = SimpleTimeMark.farPast()
        }
        hasWarned = false
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!LorenzUtils.onHypixel) return
        if (!showTime) return

        if (LorenzUtils.inSkyBlock) {
            showTime = false
        }

        if (lastKickTime.passedSince() > 5.minutes) {
            showTime = false
        }

        if (lastKickTime.passedSince() > config.warnTime.get().seconds) {
            if (!hasWarned) {
                hasWarned = true
                warn()
            }
        }

        val format = lastKickTime.passedSince().format()
        config.position.renderString(
            "§cLast kicked from SkyBlock §b$format ago",
            posLabel = "SkyBlock Kick Duration"
        )
    }

    private fun warn() {
        LorenzUtils.sendTitle("§eTry rejoining SkyBlock now!", 3.seconds)
    }

    fun isEnabled() = config.enabled
}
