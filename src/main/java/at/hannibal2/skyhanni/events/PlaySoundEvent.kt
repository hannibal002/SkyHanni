package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class PlaySoundEvent(val soundName: String, val location: LorenzVec, val pitch: Float, val volume: Float) :
    LorenzEvent() {
    val distanceToPlayer by lazy { location.distance(LocationUtils.playerLocation()) }
}