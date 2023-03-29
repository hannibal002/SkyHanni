package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.client.audio.SoundCategory
import net.minecraft.util.ResourceLocation

object SoundUtils {
    fun ISound.playSound() {
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
                        return
                    }
                }
            }
            e.printStackTrace()
        } finally {
            gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel)
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
}