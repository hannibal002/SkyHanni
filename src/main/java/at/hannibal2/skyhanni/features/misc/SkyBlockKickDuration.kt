package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkyBlockKickDuration {

    private val config get() = SkyHanniMod.feature.misc.kickDuration

    // TODO form to data class, use Resettable
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
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled || !(lastKickTime.isFarFuture())) return

        if (kickPattern.matches(event.message)) {
            if (SkyBlockUtils.onHypixel && !SkyBlockUtils.inSkyBlock) kicked()
            else kickMessage = true
        }

        if (problemJoiningPattern.matches(event.message)) kicked()
    }

    private fun notKicked() {
        showTime = false
        lastKickTime = SimpleTimeMark.farFuture()
        hasWarned = false
    }

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled || !SkyBlockUtils.onHypixel || !showTime) return

        if (SkyBlockUtils.inSkyBlock || lastKickTime.passedSince() > 5.minutes) notKicked()
        if (lastKickTime.passedSince() > config.warnTime.get().seconds && !hasWarned) warn()

        val format = lastKickTime.passedSince().format()
        config.position.renderRenderable(
            Renderable.text("§cKicked from SkyBlock §b$format ago"),
            posLabel = "SkyBlock Kick Duration",
        )
    }

    private fun warn() {
        TitleManager.sendTitle("§eTry rejoining SkyBlock now!")
        SoundUtils.playBeepSound()
        hasWarned = true
    }
}
