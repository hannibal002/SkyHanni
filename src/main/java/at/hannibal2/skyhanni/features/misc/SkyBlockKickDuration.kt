package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object SkyBlockKickDuration {

    private val config get() = HanniMod.feature.misc.kickDuration

    private var kickMessage = false
    private var showTime = false
    private var lastKickTime = SimpleTimeMark.farFuture()
    private var hasWarned = false

    private val patternGroup = RepoPattern.group("misc.kickduration")

    /**
     * REGEX-TEST: §cYou were kicked while joining that server!
     * REGEX-TEST: §cA kick occurred in your connection, so you were put in the SkyBlock lobby!
     * REGEX-TEST: §cAn exception occurred in your connection, so you were put in the SkyBlock Lobby!
     */
    @Suppress("MaxLineLength")
    private val kickPattern by patternGroup.pattern(
        "kicked",
        "§c(?:You were kicked while joining that server!|An? (?:kick|exception) occurred in your connection, so you were put in the SkyBlock [lL]obby!)",
    )

    /**
     * REGEX-TEST: §cThere was a problem joining SkyBlock, try again in a moment!
     */
    private val problemJoiningPattern by patternGroup.pattern(
        "problemjoining",
        "§cThere was a problem joining SkyBlock, try again in a moment!",
    )

    private fun kicked() {
        kickMessage = false
        showTime = true
        lastKickTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled() || !(lastKickTime.isFarFuture())) return

        if (kickPattern.matches(event.message)) {
            if (SkyBlockUtils.onHypixel && !SkyBlockUtils.inSkyBlock) {
                kicked()
            } else {
                kickMessage = true
            }
        }

        if (problemJoiningPattern.matches(event.message)) {
            kicked()
        }
    }

    private fun notKicked() {
        showTime = false
        lastKickTime = SimpleTimeMark.farFuture()
        hasWarned = false
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!SkyBlockUtils.onHypixel) return
        if (!showTime) return
        if (SkyBlockUtils.inSkyBlock) {
            notKicked()
        }

        if (lastKickTime.passedSince() > 5.minutes) {
            notKicked()
        }

        if (lastKickTime.passedSince() > config.warnTime.get().seconds) {
            if (!hasWarned) {
                hasWarned = true
                warn()
            }
        }

        val format = lastKickTime.passedSince().format()
        config.position.renderString(
            "§cKicked from SkyBlock §b$format ago",
            posLabel = "SkyBlock Kick Duration",
        )
    }

    private fun warn() {
        TitleManager.sendTitle("§eTry rejoining SkyBlock now!")
        SoundUtils.playBeepSound()
    }

    fun isEnabled() = config.enabled
}
