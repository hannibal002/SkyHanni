package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
//#if MC < 1.21
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
//#else
//$$ import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
//#endif
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
//#if MC < 1.21
import net.minecraft.util.EnumParticleTypes
//#else
//$$ import at.hannibal2.skyhanni.utils.ParticleUtils
//$$ import net.minecraft.registry.Registries
//$$ import net.minecraft.util.Identifier
//#endif

@SkyHanniModule
//#if MC < 1.21
object TrackParticlesCommand : TrackCommand<ReceiveParticleEvent, EnumParticleTypes>(
//#else
//$$ object TrackParticlesCommand : TrackCommand<ReceiveParticleEvent, Identifier>(
//#endif
    commonName = "particle",
) {
    override val config get() = SkyHanniMod.feature.dev.debug.trackParticle

    // todo add suggestion provider for particle types, maybe when we're fully in 1.21
    override val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit = {
        //#if MC < 1.21
        argCallback("name", EnumArgumentType.name<EnumParticleTypes>()) {
            handleIgnorable(it)
        }
        //#else
        //$$ argCallback("name", BrigadierArguments.string()) {
        //$$    val type = ParticleUtils.getParticleTypeByName(it, shouldError = true) ?: return@argCallback
        //$$    handleIgnorable(type)
        //$$ }
        //#endif
    }

    //#if MC < 1.21
    override fun ReceiveParticleEvent.getTypeIdentifier(): EnumParticleTypes = type
    //#else
    //$$ override fun ReceiveParticleEvent.getTypeIdentifier(): Identifier = Registries.PARTICLE_TYPE.getId(type)
    //$$    ?: throw IllegalStateException("Particle type $type is not registered in the registry")
    //#endif

    override fun ReceiveParticleEvent.formatForDisplay() = "§3${getTypeIdentifier()} §8c:$count §7s:$speed"

    override fun ReceiveParticleEvent.formatForWorldRender() = "§7C: §e$count §7S: §a${speed.roundTo(2)}"

    // No explicit filtering for particles, all particles are tracked in this context.
    override fun ReceiveParticleEvent.shouldAcceptTrackableEvent(): Boolean = true

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    fun onParticleReceive(event: ReceiveParticleEvent) = super.onTrackableEvent(event)

    @HandleEvent
    override fun onKeyPress(event: KeyPressEvent) = super.onKeyPress(event)

    @HandleEvent
    override fun onRenderWorld(event: SkyHanniRenderWorldEvent) = super.onRenderWorld(event)

    @HandleEvent
    override fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) = super.onRenderOverlay(event)

    @HandleEvent
    override fun onTick() = super.onTick()

    @HandleEvent
    override fun onCommandRegistration(event: CommandRegistrationEvent) = super.onCommandRegistration(event)

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(94, "dev.debug.trackParticlePosition", "dev.debug.trackParticle.position")
    }
}
