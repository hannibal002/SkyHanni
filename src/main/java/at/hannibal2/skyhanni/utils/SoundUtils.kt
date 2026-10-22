package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.SoundCompat
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import kotlinx.coroutines.delay

@SkyHanniModule
object SoundUtils {
    private val beepSoundCache = mutableMapOf<Float, SoundInstance>()
    private val clickSound by lazy { createSound("ui.button.click", 1f, isWarning = false) }
    private val clickWarningSound by lazy { createSound("ui.button.click", 1f, isWarning = true) }
    private val errorSound by lazy { createSound("entity.enderman.teleport", 0f, isWarning = true) }
    private val errorUiSound by lazy { createSound("entity.enderman.teleport", 0f, isWarning = false) }
    val plingSound by lazy { createSound("block.note_block.pling", 1f, isWarning = true) }

    // Warning sounds created via createSound bypass the user's volume settings
    // if the boostWarningVolume option is enabled, see SoundEngineHook.
    fun SoundInstance.playSound() {
        DelayedRun.runOrNextTick {
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
            }
        }
    }

    // isWarning controls whether the sound is affected by the boostWarningVolume option.
    // Set it to false for GUI interaction feedback and other non-warning sound effects.
    fun createSound(name: String, pitch: Float, volume: Float = 50f, isWarning: Boolean): SoundInstance {
        val newSound = SoundCompat.getModernSoundName(name)
        val identifier = Identifier.parse(newSound.replace(Regex("[^a-z0-9:/._-]"), ""))
        return SkyHanniSoundInstance(identifier, pitch, volume, isWarning)
    }

    fun playBeepSound(pitch: Float = 1f) {
        val beepSound = beepSoundCache.getOrPut(pitch) {
            createSound("entity.experience_orb.pickup", pitch, isWarning = true)
        }
        beepSound.playSound()
    }

    fun playClickSound(isWarning: Boolean = false) {
        (if (isWarning) clickWarningSound else clickSound).playSound()
    }

    fun playPlingSound() {
        plingSound.playSound()
    }

    fun playErrorSound(isWarning: Boolean = true) {
        (if (isWarning) errorSound else errorUiSound).playSound()
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
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shplaysound") {
            description = "Play the specified sound effect at the given pitch and volume."
            category = DEVELOPER_TEST
            arg("name", BrigadierArguments.string()) { soundName ->
                arg("pitch", BrigadierArguments.float()) { pitch ->
                    arg("volume", BrigadierArguments.float()) { volume ->
                        arg("warning", BrigadierArguments.bool()) { isWarning ->
                            callback {
                                createSound(
                                    getArg(soundName), getArg(pitch), getArg(volume), getArg(isWarning),
                                ).playSound()
                            }
                        }
                        callback {
                            createSound(getArg(soundName), getArg(pitch), getArg(volume), isWarning = false).playSound()
                        }
                    }
                    callback {
                        createSound(getArg(soundName), getArg(pitch), 50f, isWarning = false).playSound()
                    }
                }
                callback {
                    createSound(getArg(soundName), 1f, 50f, isWarning = false).playSound()
                }
            }
            simpleCallback {
                ChatUtils.userError("Specify a sound effect to test")
            }
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(145, "misc.maintainGameVolume", "misc.boostWarningVolume") { element ->
            JsonPrimitive(!element.asBoolean)
        }
        event.transform(146, "misc.boostWarningVolume") { _ ->
            JsonPrimitive(false)
        }
    }
}
