package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class PlaySoundEvent(val soundName: String, val location: LorenzVec, val pitch: Float, val volume: Float) :
    LorenzEvent() {
    val distanceToPlayer by lazy { location.distanceToPlayer() }
    override fun toString(): String {
        return "PlaySoundEvent(soundName='$soundName', pitch=$pitch, volume=$volume, location=$location, distanceToPlayer=${distanceToPlayer.round(1)})"
    }
}
