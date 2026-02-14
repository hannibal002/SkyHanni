package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlinx.coroutines.delay
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

@SkyHanniModule
object SoundUtils {

    private val config get() = SkyHanniMod.feature.misc
    private val beepSoundCache = mutableMapOf<Float, SoundInstance>()
    private val clickSound by lazy { createSound("gui.button.press", 1f) }
    private val errorSound by lazy { createSound("mob.endermen.portal", 0f) }
    val plingSound by lazy { createSound("note.pling", 1f) }
    val centuryActiveTimerAlert by lazy { createSound("skyhanni:centurytimer.active", 1f) }

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
        //? if < 1.21.11 {
        Minecraft.getInstance().soundManager.setVolume(this, level)
    //?} else
    //Minecraft.getInstance().soundManager.updateCategoryVolume(this.source, level)

    fun createSound(name: String, pitch: Float, volume: Float = 50f): SoundInstance {
        val newSound = at.hannibal2.skyhanni.utils.compat.SoundCompat.getModernSoundName(name)
        val identifier = Identifier.parse(newSound.replace(Regex("[^a-z0-9/._-]"), ""))
        return SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(identifier), pitch, volume)
    }

    fun playBeepSound(pitch: Float = 1f) {
        val beepSound = beepSoundCache.getOrPut(pitch) { createSound("random.orb", pitch) }
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
                            createSound(getArg(soundName), getArg(pitch), getArg(volume)).playSound()
                        }
                    }
                    callback {
                        createSound(getArg(soundName), getArg(pitch), 50f).playSound()
                    }
                }
                callback {
                    createSound(getArg(soundName), 1f, 50f).playSound()
                }
            }
            simpleCallback {
                ChatUtils.userError("Specify a sound effect to test")
            }
        }
    }
}
