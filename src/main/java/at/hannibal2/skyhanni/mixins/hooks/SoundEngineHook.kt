package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.SkyHanniSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance

object SoundEngineHook {
    /**
     * Makes SkyHanni's own sounds bypass the user's volume settings, without touching
     * the volume of any other sound or the sound engine's global volume state.
     */
    @JvmStatic
    fun modifySoundVolume(soundInstance: SoundInstance, original: Float): Float {
        if (soundInstance !is SkyHanniSoundInstance) return original
        if (SkyHanniMod.feature.misc.maintainGameVolume) return original
        return soundInstance.volume.coerceIn(0f, 1f)
    }
}
