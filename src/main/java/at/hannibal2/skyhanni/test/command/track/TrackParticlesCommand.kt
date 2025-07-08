package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.track.TrackParticlesCommand.ignoredTypes
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ParticleUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
//#if MC < 1.21
import net.minecraft.util.EnumParticleTypes
//#endif
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
//#if MC > 1.21
//$$ import net.minecraft.particle.ParticleType
//#endif

@SkyHanniModule
//#if MC < 1.21
object TrackParticlesCommand : TrackCommand<ReceiveParticleEvent, EnumParticleTypes>(
//#else
//$$ object TrackParticlesCommand : TrackCommand<ReceiveParticleEvent, ParticleType<*>>(
//#endif
    commonName = "particle",
) {
    override val config get() = SkyHanniMod.feature.dev.debug.trackParticle

    //#if MC < 1.21
    override fun ReceiveParticleEvent.getGroupKey(): EnumParticleTypes = type
    //#else
    //$$ override fun ReceiveParticleEvent.getGroupKey(): ParticleType<*> = type
    //#endif

    override fun drawDisplay(tracked: List<Pair<Duration, ReceiveParticleEvent>>): List<Renderable> =
        tracked.take(10).reversed().map {
            StringRenderable("§3" + it.second.type + " §8c:" + it.second.count + " §7s:" + it.second.speed)
        }

    //#if MC < 1.21
    private val ignoredTypes = mutableListOf<EnumParticleTypes>()
    //#else
    //$$ private val ignoredTypes = mutableListOf<ParticleType<*>>()
    //#endif

    override fun earlyArgHandler(args: Array<String>, isRecording: Boolean): Boolean {
        //#if TODO
        if (args.isEmpty()) return false
        val type = ParticleUtils.getParticleTypeByName(args[0]) ?: run {
            ChatUtils.userError("Unknown particle type: '${args[0]}'")
            return true
        }
        if (ignoredTypes.contains(type)) {
            ignoredTypes.remove(type)
            ChatUtils.chat("Removed $type from ignored types.")
        } else {
            ignoredTypes.add(type)
            ChatUtils.chat("Added $type to ignored types.")
        }
        return true
        //#else
        //$$ return false
        //#endif
    }

    @HandleEvent
    override fun onTrackable(event: ReceiveParticleEvent) {
        if (cutOffTime.isInPast()) return
        //#if TODO
        if (event.type in ignoredTypes) return
        //#endif
        event.distanceToPlayer // Need to call to initialize Lazy
        addTrackable(event)
    }

    override fun SkyHanniRenderWorldEvent.drawSingle(vec: LorenzVec, event: ReceiveParticleEvent) {
        drawDynamicText(vec, "§7§l${event.type}", 0.8)
        drawDynamicText(
            vec.down(0.2),
            "§7C: §e${event.count} §7S: §a${event.speed.roundTo(2)}",
            scaleMultiplier = 0.8,
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(92, "dev.debug.trackParticlePosition", "dev.debug.trackParticle.position")
    }
}
