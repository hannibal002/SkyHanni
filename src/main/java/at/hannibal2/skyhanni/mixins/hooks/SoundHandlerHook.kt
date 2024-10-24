package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.inSkyBlock
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.client.audio.SoundEventAccessorComposite
import net.minecraft.client.audio.SoundManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.ForgeHooksClient
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

var soundMap: HashMap<String, Boolean> = hashMapOf()

fun getSoundName(sound: ISound, soundManager: SoundManager): String {
    val oldSoundName = sound.soundLocation.toString()
    val newSoundName = SkyHanniMod.feature.misc.replaceAllSounds.soundName
    if (soundMap[newSoundName] == true) {
        return newSoundName
    } else if (soundMap[newSoundName] == false) {
        return oldSoundName
    }

    //stolen from Minecrafts SoundHandler
    val p_sound: ISound? = ForgeHooksClient.playSound(soundManager, sound)
    if (p_sound == null) {
        soundMap[newSoundName] = false
        return oldSoundName
    }


    val soundeventaccessorcomposite: SoundEventAccessorComposite? = soundManager.sndHandler.getSound(ResourceLocation(newSoundName))
    if (soundeventaccessorcomposite == null) {
        soundMap[newSoundName] = false
        return oldSoundName
    }
    soundMap[newSoundName] = true
    return newSoundName

}

fun playSoundHook(sound: ISound, ci: CallbackInfo, soundManager: SoundManager) {
    val enabled = SkyHanniMod.feature.misc.replaceAllSounds.enabled
    var isOnlyAprilFools = !enabled && LorenzUtils.isAprilFoolsDay
    if (inSkyBlock && (isOnlyAprilFools || enabled)) {
        var resourceLocation = ResourceLocation(getSoundName(sound, soundManager))
        if (LorenzUtils.isAprilFoolsDay) {
            val random = (0..1_000_000).random()
            if (random == 0) {
                resourceLocation = ResourceLocation("mob.guardian.curse")
                isOnlyAprilFools = false
            } else if (random < 5) {
                resourceLocation = ResourceLocation("mob.ghast.moan")
                isOnlyAprilFools = false
            } else if (random < 15) {
                resourceLocation = ResourceLocation("mob.endermen.death")
                isOnlyAprilFools = false
            } else if (random < 115) {
                resourceLocation = ResourceLocation("mob.guardian.flop")
                isOnlyAprilFools = false
            }
        }
        if (isOnlyAprilFools) return
        val newSound: ISound = object : PositionedSound(resourceLocation) {
            init {
                volume = sound.getVolume()
                pitch = sound.getPitch()
                repeat = sound.canRepeat()
                repeatDelay = sound.getRepeatDelay()
                attenuationType = sound.getAttenuationType()
                xPosF = sound.getXPosF()
                yPosF = sound.getYPosF()
                zPosF = sound.getZPosF()
            }
        }
        soundManager.playSound(newSound)
        ci.cancel()
    }
}
