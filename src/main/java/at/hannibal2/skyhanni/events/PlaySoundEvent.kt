package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo

class PlaySoundEvent(val rawSoundName: String, val location: LorenzVec, val pitch: Float, val volume: Float) : CancellableSkyHanniEvent() {

    val soundName by lazy { getSoundName(rawSoundName) }

    val distanceToPlayer by lazy { location.distanceToPlayer() }
    override fun toString(): String {
        return "PlaySoundEvent(soundName='$soundName', pitch=$pitch, volume=$volume, location=${location.roundTo(1)}, distanceToPlayer=${
            distanceToPlayer.roundTo(2)
        })"
    }

    companion object {
        private fun getSoundName(rawSoundName: String): String {
            //#if MC < 1.21
            return rawSoundName
            //#else
            //$$ return at.hannibal2.skyhanni.utils.compat.SoundCompat.getLegacySoundName(rawSoundName)
            //#endif
        }
    }
}
