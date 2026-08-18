package at.hannibal2.skyhanni.utils

import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

/**
 * A UI sound created by SkyHanni. Unless the user opted out via the Maintain Volume During Warnings option,
 * these sounds bypass the volume settings and play at their instance volume, without affecting any other sounds.
 * See `MixinSoundEngine` and [at.hannibal2.skyhanni.mixins.hooks.SoundEngineHook].
 */
class SkyHanniSoundInstance(
    soundEvent: SoundEvent,
    pitch: Float,
    volume: Float,
) : SimpleSoundInstance(
    soundEvent.location(),
    SoundSource.UI,
    volume,
    pitch,
    SoundInstance.createUnseededRandom(),
    false,
    0,
    SoundInstance.Attenuation.NONE,
    0.0,
    0.0,
    0.0,
    true,
)
