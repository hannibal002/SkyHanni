package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
//#if MC < 1.21
import net.minecraft.util.EnumParticleTypes
//#else
//$$ import net.minecraft.particle.ParticleType
//#endif
import kotlin.time.Duration

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
    override fun ReceiveParticleEvent.getTypeIdentifier(): EnumParticleTypes = type
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

    // todo add suggestion provider for particle types, maybe when we're fully in 1.21
    override val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit = {
        //#if MC < 1.21
        argCallback("name", EnumArgumentType.name<EnumParticleTypes>()) {
            handleIgnorable(it)
        }
        //#else
        //$$ argCallback("name", BrigadierArguments.string()) {
        //$$    val type = ParticleUtils.getParticleTypeByName(it) ?: return@argCallback
        //$$    handleIgnorable(type)
        //$$ }
        //#endif
    }

    override fun onTrackable(event: ReceiveParticleEvent) {
        if (cutOffTime.isInPast()) return
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
