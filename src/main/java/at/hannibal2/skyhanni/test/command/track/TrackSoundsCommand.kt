package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.DevApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.core.registries.BuiltInRegistries

@SkyHanniModule
object TrackSoundsCommand : TrackWorldCommand<PlaySoundEvent, String>(commonName = "sound") {

    override val config get() = DevApi.config.debug.trackSound

    override val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit = {
        argCallback("sound_name", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { allSoundIds }) {
            val soundName = it.trim()
            if (soundName.isEmpty()) {
                ChatUtils.chat("§cSound name cannot be empty")
                return@argCallback
            }
            handleIgnorable(soundName)
        }
    }

    override fun PlaySoundEvent.getTypeIdentifier() = soundName

    override fun PlaySoundEvent.formatForDisplay() = Renderable.text("§3$soundName §8p:$pitch §7v:$volume")

    override fun PlaySoundEvent.formatForWorldRender(): String {
        val volumeColor = when (volume) {
            in 0.0..0.25 -> "§c"
            in 0.25..0.5 -> "§6"
            else -> "§a"
        }
        return "§7P: §e${pitch.roundTo(2)} §7V: $volumeColor${volume.roundTo(2)}"
    }

    override fun PlaySoundEvent.shouldAcceptTrackableEvent(): Boolean = when {
        soundName == "game.player.hurt" && pitch == 0f && volume == 0f -> false // remove random useless sound
        soundName.isEmpty() -> false // sound with empty name aren't useful
        else -> if (MinecraftCompat.localPlayerExists) {
            distanceToPlayer // Need to call to initialize Lazy
            true
        } else false
    }

    // PlaySoundEvent.soundName strips the namespace prefix, so suggestions mirror that
    private val allSoundIds: List<String> by lazy {
        BuiltInRegistries.SOUND_EVENT.keySet()
            .map { it.toString().removePrefix("minecraft:") }
            .sorted()
    }

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    fun onPlaySound(event: PlaySoundEvent) = super.onTrackableEvent(event)

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
        event.move(94, "dev.debug.trackSoundPosition", "dev.debug.trackSound.position")
    }
}
