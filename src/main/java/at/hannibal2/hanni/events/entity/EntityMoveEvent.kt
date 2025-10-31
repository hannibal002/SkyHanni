package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.GenericHanniEvent
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.compat.MinecraftCompat.isLocalPlayer
import net.minecraft.entity.EntityLivingBase

class EntityMoveEvent<T : EntityLivingBase>(
    val entity: T,
    val oldLocation: LorenzVec,
    val newLocation: LorenzVec,
    val distance: Double,
) : GenericHanniEvent<T>(entity.javaClass) {
    val isLocalPlayer get() = entity.isLocalPlayer
}
