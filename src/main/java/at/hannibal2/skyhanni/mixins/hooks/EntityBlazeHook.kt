package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.BlazeParticleEvent
import net.minecraft.entity.monster.EntityBlaze

fun onBlockBlazeParticle(mixinBlaze: Any): Boolean {
    val blaze = mixinBlaze as EntityBlaze
    return BlazeParticleEvent(blaze).postAndCatch()
}
