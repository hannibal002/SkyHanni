package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlinx.coroutines.delay
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.ResourceLocation
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
                    "soundLocation" to this.location,
                )
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to play a sound",
                    "soundLocation" to this.location,
                )
            } finally {
                if (!config.maintainGameVolume) this.setLevel(oldLevel)
            }
        }
    }

    private fun SoundInstance.setLevel(level: Float) =
        //#if MC < 1.21.9
        Minecraft.getInstance().soundManager.updateSourceVolume(this.source, level)
    //#else
    //$$ Minecraft.getInstance().soundManager.setVolume(this, level)
    //#endif

    fun createSound(name: String, pitch: Float, volume: Float = 50f): SoundInstance {
        val newSound = at.hannibal2.skyhanni.utils.compat.SoundCompat.getModernSoundName(name)
        val identifier = ResourceLocation.parse(newSound.replace(Regex("[^a-z0-9/._-]"), ""))
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

    private fun onCommand(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.userError("Specify a sound effect to test")
            return
        }

        val soundName = args[0]
        val pitch = args.getOrNull(1)?.toFloat() ?: 1f
        val volume = args.getOrNull(2)?.toFloat() ?: 50f

        createSound(soundName, pitch, volume).playSound()
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
            legacyCallbackArgs { onCommand(it) }
        }
    }
}
