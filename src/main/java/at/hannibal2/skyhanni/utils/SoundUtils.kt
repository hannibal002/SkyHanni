package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.SoundCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.AudioStream
import net.minecraft.client.sounds.SoundBufferLibrary
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.delay

@SkyHanniModule
object SoundUtils {

    private val config get() = SkyHanniMod.feature.misc
    private val beepSoundCache = mutableMapOf<Float, SoundInstance>()
    private val clickSound by lazy { createSound("ui.button.click", 1f) }
    private val errorSound by lazy { createSound("entity.enderman.teleport", 0f) }
    val plingSound by lazy { createSound("block.note_block.pling", 1f) }

    fun SoundInstance.playSound() {
        DelayedRun.runOrNextTick {
            val category = this.source

            val oldLevel = Minecraft.getInstance().options.getSoundSourceVolume(category)
            if (!config.maintainGameVolume) this.setLevel(1f)

            try {
                Minecraft.getInstance().soundManager.play(this)
            } catch (e: IllegalArgumentException) {
                if (e.message?.startsWith("value already present:") == true) return@runOrNextTick
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to play a sound",
                    "soundLocation" to this.identifier,
                )
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to play a sound",
                    "soundLocation" to this.identifier,
                )
            } finally {
                if (!config.maintainGameVolume) this.setLevel(oldLevel)
            }
        }
    }

    private fun SoundInstance.setLevel(level: Float) =
        Minecraft.getInstance().soundManager.updateCategoryVolume(source, level)

    fun createSound(name: String, pitch: Float, volume: Float = 1f, bypassVolumeMaximum: Boolean = false): SoundInstance {
        val newSound = SoundCompat.getModernSoundName(name)
        val identifier = Identifier.parse(newSound.replace(Regex("[^a-z0-9:/._-]"), ""))
        val sound = SimpleSoundInstance(
            identifier,
            SoundSource.UI,
            volume.coerceIn(0f, 1000f),
            pitch,
            SoundInstance.createUnseededRandom(),
            false,
            0,
            SoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            false, // Should not be relative to position
        )

        return if (bypassVolumeMaximum) {
            BypassMaximumVolumeSound(sound)
        } else {
            sound
        }
    }

    fun playBeepSound(pitch: Float = 1f) {
        val beepSound = beepSoundCache.getOrPut(pitch) { createSound("entity.experience_orb.pickup", pitch) }
        beepSound.playSound()
    }

    fun playClickSound() {
        clickSound.playSound()
    }

    fun playPlingSound() {
        plingSound.playSound()
    }

    fun playErrorSound() {
        errorSound.playSound()
    }

    // TODO use duration for delay
    fun repeatSound(delay: Long, repeat: Int, sound: SoundInstance) {
        SkyHanniMod.launchCoroutine("repeatSound") {
            repeat(repeat) {
                sound.playSound()
                delay(delay)
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shplaysound") {
            description = "Play the specified sound effect at the given pitch and volume."
            category = CommandCategory.DEVELOPER_TEST
            arg("name", BrigadierArguments.string()) { soundName ->
                arg("pitch", BrigadierArguments.float()) { pitch ->
                    arg("volume", BrigadierArguments.float()) { volume ->
                        callback {
                            createSound(
                                getArg(soundName),
                                getArg(pitch),
                                getArg(volume)
                            ).playSound()
                        }
                        arg("bypassVolumeMaximum", BrigadierArguments.bool()) { bypassVolumeMaximum ->
                            callback {
                                createSound(
                                    getArg(soundName),
                                    getArg(pitch),
                                    getArg(volume),
                                    getArg(bypassVolumeMaximum)
                                ).playSound()
                            }
                        }
                    }
                    callback {
                        createSound(getArg(soundName), getArg(pitch), 1f).playSound()
                    }
                }
                callback {
                    createSound(getArg(soundName), 1f, 1f).playSound()
                }
            }
            simpleCallback {
                ChatUtils.userError("Specify a sound effect to test")
            }
        }
    }
}

class BypassMaximumVolumeSound(val delegate: SoundInstance) : SoundInstance by delegate {
    override fun canStartSilent(): Boolean {
        return delegate.canStartSilent()
    }

    override fun canPlaySound(): Boolean {
        return delegate.canPlaySound()
    }

    override fun getAudioStream(
        library: SoundBufferLibrary,
        id: Identifier,
        repeatInstantly: Boolean,
    ): CompletableFuture<AudioStream> {
        return delegate.getAudioStream(library, id, repeatInstantly)
    }
}
