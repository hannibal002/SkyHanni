package at.hannibal2.skyhanni.test.command


import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

object TrackSoundsCommand {

    var cutOfTime: SimpleTimeMark = SimpleTimeMark.farPast()

    val sounds = mutableListOf<PlaySoundEvent>()

    var enable = AtomicBoolean(true)

    fun command(args: Array<String>) {
        if (!enable.get()) {
            LorenzUtils.chat("Still tracking sounds, wait for the other tracking to complete before starting a new one")
            return
        }
        enable.set(false)
        sounds.clear()
        val duration = if (args.size == 1) {
            args[0].toInt().seconds
        } else {
            5.0.seconds
        }
        LorenzUtils.chat("Now started tracking sounds for ${duration.inWholeSeconds} Seconds")
        cutOfTime = SimpleTimeMark.future(duration)
        DelayedRun.runDelayed(duration + 0.1.seconds) {
            val string = sounds.joinToString("\n") { it.toString() }
            val counter = sounds.size
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("$counter sounds copied into the clipboard!")
            enable.set(true)
        }
    }

    @SubscribeEvent
    fun onSoundEvent(event: PlaySoundEvent) {
        if (cutOfTime.isInPast()) return
        event.distanceToPlayer // Need to call to initialize Lazy
        sounds.add(event)
    }
}
