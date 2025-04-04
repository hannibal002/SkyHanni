package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundCategory
import net.minecraft.util.ResourceLocation
//#if MC < 1.21
import net.minecraft.client.audio.PositionedSound
//#else
//$$ import net.minecraft.client.sound.PositionedSoundInstance
//$$ import net.minecraft.sound.SoundEvent
//#endif

object SoundUtils {

    private val beepSoundCache = mutableMapOf<Float, ISound>()
    private val clickSound by lazy { createSound("gui.button.press", 1f) }
    private val errorSound by lazy { createSound("mob.endermen.portal", 0f) }
    val plingSound by lazy { createSound("note.pling", 1f) }
    val centuryActiveTimerAlert by lazy { createSound("skyhanni:centurytimer.active", 1f) }

    fun ISound.playSound() {
        DelayedRun.onThread.execute {
            val oldLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.PLAYERS)
            if (!SkyHanniMod.feature.misc.maintainGameVolume) {
                Minecraft.getMinecraft().soundHandler.setSoundLevel(SoundCategory.PLAYERS, 1f)
            }
            try {
                Minecraft.getMinecraft().soundHandler.playSound(this)
            } catch (e: IllegalArgumentException) {
                if (e.message?.startsWith("value already present:") == true) return@execute
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to play a sound",
                    "soundLocation" to this.soundLocation,
                )
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to play a sound",
                    "soundLocation" to this.soundLocation,
                )
            } finally {
                if (!SkyHanniMod.feature.misc.maintainGameVolume) {
                    Minecraft.getMinecraft().soundHandler.setSoundLevel(SoundCategory.PLAYERS, oldLevel)
                }
            }
        }
    }

    fun createSound(name: String, pitch: Float, volume: Float = 50f): ISound {
        //#if MC < 1.21
        val sound: ISound = object : PositionedSound(ResourceLocation(name)) {
            init {
                this.volume = volume
                repeat = false
                repeatDelay = 0
                attenuationType = ISound.AttenuationType.NONE
                this.pitch = pitch
            }
        }
        return sound
        //#else
        //$$ return PositionedSoundInstance.master(SoundEvent.of(Identifier.of(name)), pitch, volume)
        //#endif
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

    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.userError("Specify a sound effect to test")
            return
        }

        val soundName = args[0]
        val pitch = args.getOrNull(1)?.toFloat() ?: 1.0f
        val volume = args.getOrNull(2)?.toFloat() ?: 50.0f

        createSound(soundName, pitch, volume).playSound()
    }

    fun playErrorSound() {
        errorSound.playSound()
    }

    // TODO use duration for delay
    fun repeatSound(delay: Long, repeat: Int, sound: ISound) {
        SkyHanniMod.coroutineScope.launch {
            repeat(repeat) {
                sound.playSound()
                delay(delay)
            }
        }
    }
}
