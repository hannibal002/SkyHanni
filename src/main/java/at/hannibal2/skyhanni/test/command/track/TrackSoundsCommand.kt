package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import kotlin.ranges.contains

@SkyHanniModule
object TrackSoundsCommand : TrackCommand<PlaySoundEvent, String>(
    commonName = "sound"
) {
    override val config get() = SkyHanniMod.feature.dev.debug.trackSound

    // todo change this from a string arg to a qualified sound name arg
    // todo add suggestion provider for sound names
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

    override fun PlaySoundEvent.formatForDisplay() = "§3$soundName §8p:$pitch §7v:$volume"

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
        else -> {
            distanceToPlayer // Need to call to initialize Lazy
            true
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST, receiveCancelled = true)
    fun onPlaySound(event: PlaySoundEvent) = super.onTrackableEvent(event)

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
        event.move(94, "dev.debug.trackSoundPosition", "dev.debug.trackSound.position")
    }
}
