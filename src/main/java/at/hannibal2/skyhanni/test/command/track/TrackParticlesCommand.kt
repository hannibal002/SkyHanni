package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.DevApi
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.ParticleUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

@SkyHanniModule
object TrackParticlesCommand : TrackWorldCommand<ReceiveParticleEvent, Identifier>(commonName = "particle") {

    override val config get() = DevApi.config.debug.trackParticle

    override val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit = {
        argCallback("name", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { allParticleIds }) {
            val type = ParticleUtils.getParticleTypeByName(it, shouldError = true) ?: return@argCallback
            handleIgnorable(type)
        }
    }

    override fun ReceiveParticleEvent.getTypeIdentifier(): Identifier = BuiltInRegistries.PARTICLE_TYPE.getKey(type)
        ?: throw IllegalStateException("Particle type $type is not registered in the registry")

    override fun ReceiveParticleEvent.formatForDisplay() = Renderable.text("§3${getTypeIdentifier()} §8c:$count §7s:$speed")

    override fun ReceiveParticleEvent.formatForWorldRender() = "§7C: §e$count §7S: §a${speed.roundTo(2)}"

    // No explicit filtering for particles, all particles are tracked in this context.
    override fun ReceiveParticleEvent.shouldAcceptTrackableEvent(): Boolean = true

    private val allParticleIds: List<String> by lazy {
        BuiltInRegistries.PARTICLE_TYPE.keySet().map { it.toString() }.sorted()
    }

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    fun onParticleReceive(event: ReceiveParticleEvent) = super.onTrackableEvent(event)

    // TODO for DavidArthurCole, this whole structure seems unnecessary.
    //  We're defining event handlers that defer to inherits, in the same shape
    @HandleEvent
    override fun onKeyPress(event: KeyPressEvent) = super.onKeyPress(event)

    @HandleEvent
    override fun onRenderWorld(event: SkyHanniRenderWorldEvent) = super.onRenderWorld(event)

    @HandleEvent
    override fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) = super.onGuiRenderOverlay(event)

    @HandleEvent
    override fun onTick() = super.onTick()

    @HandleEvent
    override fun onCommandRegistration(event: CommandRegistrationEvent) = super.onCommandRegistration(event)

    @HandleEvent
    override fun onDisconnect(event: ClientDisconnectEvent) = super.onDisconnect(event)

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(94, "dev.debug.trackParticlePosition", "dev.debug.trackParticle.position")
    }
}
