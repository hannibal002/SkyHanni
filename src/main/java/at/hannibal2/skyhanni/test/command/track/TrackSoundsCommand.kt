package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.collections.iterator
import kotlin.ranges.contains
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TrackSoundsCommand : TrackCommand<PlaySoundEvent, String>(
    commonName = "sound"
) {
    override val config get() = SkyHanniMod.feature.dev.debug.trackSound

    override val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit = {
        argCallback("sound_name", BrigadierArguments.string()) {
            val soundName = it.trim()
            if (soundName.isEmpty()) {
                ChatUtils.chat("§cSound name cannot be empty")
                return@argCallback
            }
            handleIgnorable(soundName)
        }
    }

    override fun PlaySoundEvent.getTypeIdentifier() = soundName

    override fun drawDisplay(tracked: List<Pair<Duration, PlaySoundEvent>>): List<Renderable> =
        tracked.take(10).reversed().map {
            StringRenderable("§3" + it.second.soundName + " §8p:" + it.second.pitch + " §7v:" + it.second.volume)
        }

    override fun SkyHanniRenderWorldEvent.drawSingle(vec: LorenzVec, event: PlaySoundEvent) {
        val volumeColor = when (event.volume) {
            in 0.0..0.25 -> "§c"
            in 0.25..0.5 -> "§6"
            else -> "§a"
        }

        drawDynamicText(vec, "§7§l${event.soundName}", 0.8)
        drawDynamicText(
            vec.down(0.2),
            "§7P: §e${event.pitch.roundTo(2)} §7V: $volumeColor${event.volume.roundTo(2)}",
            scaleMultiplier = 0.8,
        )
    }

    @HandleEvent
    override fun onTrackable(event: PlaySoundEvent) {
        if (event.soundName == "game.player.hurt" && event.pitch == 0f && event.volume == 0f) return // remove random useless sound
        if (event.soundName == "") return // sound with empty name aren't useful
        event.distanceToPlayer // Need to call to initialize Lazy
        addTrackable(event)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(92, "dev.debug.trackSoundPosition", "dev.debug.trackSound.position")
    }
}
