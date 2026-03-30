package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound

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
