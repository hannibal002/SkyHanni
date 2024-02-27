package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object TrackSoundsCommand {

    private var cutOfTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var startTime: SimpleTimeMark = SimpleTimeMark.farPast()

    private val sounds = ConcurrentLinkedDeque<Pair<Duration, PlaySoundEvent>>()

    private var isRecording = false

    private val position get() = SkyHanniMod.feature.dev.debug.trackSoundLog

    fun command(args: Array<String>) {
        if (args.firstOrNull() == "end") {
            if (!isRecording) {
                ChatUtils.userError("Nothing to end")
            } else {
                cutOfTime = SimpleTimeMark.now()
            }
            return
        }
        if (isRecording) {
            ChatUtils.userError("Still tracking sounds, wait for the other tracking to complete before starting a new one or type in \"end\" to end it prematurely")
            return
        }
        isRecording = true
        sounds.clear()
        val duration: Duration?
        if (args.firstOrNull()?.isInt() != true) {
            duration = null
            ChatUtils.chat("Now started tracking sounds until manually ended")
        } else {
            duration = args.firstOrNull()?.toInt()?.seconds
            ChatUtils.chat("Now started tracking sounds for ${duration?.inWholeSeconds} Seconds")
        }
        startTime = SimpleTimeMark.now()
        cutOfTime = duration?.let { SimpleTimeMark.future(it) } ?: SimpleTimeMark.farFuture()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isRecording) return
        // The function must run after cutOfTime has passed to ensure thread safety
        if (cutOfTime.passedSince() <= 0.1.seconds) return
        val string = sounds.reversed().joinToString("\n") { "Time: ${it.first.inWholeMilliseconds}  ${it.second}" }
        val counter = sounds.size
        OSUtils.copyToClipboard(string)
        ChatUtils.chat("$counter sounds copied into the clipboard!")
        sounds.clear()
        isRecording = false
    }

    @SubscribeEvent
    fun onSoundEvent(event: PlaySoundEvent) {
        if (cutOfTime.isInPast()) return
        if (event.soundName == "game.player.hurt" && event.pitch == 0f && event.volume == 0f) return // remove random useless sound
        if (event.soundName == "") return // sound with empty name aren't useful
        event.distanceToPlayer // Need to call to initialize Lazy
        sounds.addFirst(startTime.passedSince() to event)
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (cutOfTime.isInPast()) return
        val list = sounds.takeWhile { startTime.passedSince() - it.first < 3.0.seconds }
            .take(10).reversed().map {
                Renderable.string("§3" + it.second.soundName + " §8p:" + it.second.pitch + " §7v:" + it.second.volume)
            }
        position.renderRenderables(list, posLabel = "Track sound log")
    }
}
