package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SkyhanniChatEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkyBlockKickDuration {

    private val config get() = SkyHanniMod.feature.misc.kickDuration

    private var kickMessage = false
    private var showTime = false
    private var lastKickTime = SimpleTimeMark.farPast()
    private var hasWarned = false

    @HandleEvent
    fun onChat(event: SkyhanniChatEvent) {
        if (!isEnabled()) return
        if (event.message == "§cYou were kicked while joining that server!") {

            if (LorenzUtils.onHypixel && !LorenzUtils.inSkyBlock) {
                kickMessage = false
                showTime = true
                lastKickTime = SimpleTimeMark.now()
            } else {
                kickMessage = true
            }
        }

        if (event.message == "§cThere was a problem joining SkyBlock, try again in a moment!") {
            kickMessage = false
            showTime = true
            lastKickTime = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!isEnabled()) return
        if (kickMessage) {
            kickMessage = false
            showTime = true
            lastKickTime = SimpleTimeMark.now()
        }
        hasWarned = false
    }

    @HandleEvent
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
        SoundUtils.playBeepSound()
    }

    fun isEnabled() = config.enabled
}
