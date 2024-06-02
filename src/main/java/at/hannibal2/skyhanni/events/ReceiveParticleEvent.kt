package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class ReceiveParticleEvent(
    val type: EnumParticleTypes,
    val location: LorenzVec,
    val count: Int,
    val speed: Float,
    val offset: LorenzVec,
    val longDistance: Boolean,
    val particleArgs: IntArray,
) :
    LorenzEvent() {

    val distanceToPlayer by lazy { location.distanceToPlayer() }

    override fun toString(): String {
        return "ReceiveParticleEvent(type='$type', location=${location.round(1)}, count=$count, speed=$speed, offset=${
            offset.round(
                1
            )
        }, longDistance=$longDistance, particleArgs=${particleArgs.contentToString()}, distanceToPlayer=${
            distanceToPlayer.round(
                1
            )
        })"
    }
}
