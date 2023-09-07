package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.client.audio.SoundCategory
import net.minecraft.util.ResourceLocation

object SoundUtils {
    private val beepSound by lazy { createSound("random.orb", 1f) }
    private val clickSound by lazy { createSound("gui.button.press", 1f) }
    val centuryActiveTimerAlert by lazy { createSound("skyhanni:centurytimer.active", 1f) }

    fun ISound.playSound() {
        Minecraft.getMinecraft().addScheduledTask {
            val gameSettings = Minecraft.getMinecraft().gameSettings
            val oldLevel = gameSettings.getSoundLevel(SoundCategory.PLAYERS)
            gameSettings.setSoundLevel(SoundCategory.PLAYERS, 1f)
            try {
                Minecraft.getMinecraft().soundHandler.playSound(this)
            } catch (e: Exception) {
                if (e is IllegalArgumentException) {
                    e.message?.let {
                        if (it.startsWith("value already present:")) {
                            println("SkyHanni Sound error: $it")
                            return@addScheduledTask
                        }
                    }
                }
                e.printStackTrace()
            } finally {
                gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel)
            }
        }
    }

    fun createSound(name: String, pitch: Float): ISound {
        val sound: ISound = object : PositionedSound(ResourceLocation(name)) {
            init {
                volume = 50f
                repeat = false
                repeatDelay = 0
                attenuationType = ISound.AttenuationType.NONE
                this.pitch = pitch
            }
        }
        return sound
    }

    fun playBeepSound() {
        beepSound.playSound()
    }

    fun playClickSound() {
        clickSound.playSound()
    }
}