package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SkyBlockKickDuration {

    private val config get() = SkyHanniMod.feature.misc.kickDuration

    private var kickMessage = false
    private var showTime = false
    private var lastKickTime = SimpleTimeMark.farPast()
    private var hasWarned = false

    private val patternGroup = RepoPattern.group("skyblockkickduration")
    private val kickedPattern by patternGroup.pattern(
        "kicked",
        "§cYou were kicked while joining that server!"
    )
    private val problemPattern by patternGroup.pattern(
        "problem",
        "§cThere was a problem joining SkyBlock, try again in a moment!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (kickedPattern.matches(event.message)) {

            if (LorenzUtils.onHypixel && !LorenzUtils.inSkyBlock) {
                kickMessage = false
                showTime = true
                lastKickTime = SimpleTimeMark.now()
            } else {
                kickMessage = true
            }
        }

        if (problemPattern.matches(event.message)) {
            kickMessage = false
            showTime = true
            lastKickTime = SimpleTimeMark.now()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!isEnabled()) return
        if (kickMessage) {
            kickMessage = false
            showTime = true
            lastKickTime = SimpleTimeMark.now()
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
        SoundUtils.playBeepSound()
    }

    fun isEnabled() = config.enabled
}
