package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import net.minecraft.util.EnumParticleTypes
//#if MC > 1.21
//$$ import net.minecraft.particle.ParticleType
//$$ import net.minecraft.registry.Registries
//#endif

class ReceiveParticleEvent(
    //#if MC < 1.21
    val type: EnumParticleTypes,
    //#else
    //$$ val type: ParticleType<*>,
    //#endif
    val location: LorenzVec,
    val count: Int,
    val speed: Float,
    val offset: LorenzVec,
    private val longDistance: Boolean,
    private val particleArgs: IntArray? = null,
) : CancellableSkyHanniEvent() {

    val distanceToPlayer by lazy { location.distanceToPlayer() }

    //#if FORGE
    override fun toString(): String {
        return "ReceiveParticleEvent(type='$type', location=${location.roundTo(1)}, count=$count, speed=$speed, offset=${
            offset.roundTo(
                1
            )
        }, longDistance=$longDistance, particleArgs=${particleArgs.contentToString()}, distanceToPlayer=${
            distanceToPlayer.roundTo(
                1
            )
        })"
    }
//#else
//$$ override fun toString(): String {
//$$          return "ReceiveParticleEvent(type='${Registries.PARTICLE_TYPE.getId(type)}', location=${location.roundTo(1)}, count=$count, speed=$speed, offset=${
//$$              offset.roundTo(
//$$                  1
//$$              )
//$$          }, longDistance=$longDistance, distanceToPlayer=${
//$$              distanceToPlayer.roundTo(
//$$                  1
//$$              )
//$$          })"
//$$      }
//#endif
}
