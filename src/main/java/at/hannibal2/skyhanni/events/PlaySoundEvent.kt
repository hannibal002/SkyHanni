package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound

/**
 * Fired when the client is about to play a sound.
 *
 * Fired on the main client thread via a Mixin into `SoundEngine.play`.
 *
 * This event is cancellable. Cancelling it prevents the sound from being played.
 * Use [replaceWithOther] to cancel the current sound and play a different one instead.
 *
 * @param soundName the Minecraft sound identifier, without the `minecraft:` namespace prefix
 * @param location the world position where the sound originates
 * @param pitch the pitch of the sound
 * @param volume the volume of the sound
 */
@PrimaryFunction("onPlaySound")
class PlaySoundEvent(
    val soundName: String,
    override val location: LorenzVec,
    val pitch: Float,
    val volume: Float,
) : CancellableWorldEvent() {

    val distanceToPlayer by lazy { location.distanceToPlayer() }
    override fun toString(): String {
        return "PlaySoundEvent(soundName='$soundName', pitch=$pitch, volume=$volume, location=${location.roundTo(1)}, distanceToPlayer=${
            distanceToPlayer.roundTo(2)
        })"
    }

    /**
     * Cancels the current event, and plays the replacement sound with the same pitch and volume.
     */
    fun replaceWithOther(soundName: String) {
        this.cancel()
        SoundUtils.createSound(soundName, pitch, volume).playSound()
    }
}
