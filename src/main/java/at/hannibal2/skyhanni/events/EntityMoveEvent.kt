package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.entity.Entity

class EntityMoveEvent(
    val entity: Entity,
    val oldLocation: LorenzVec,
    val newLocation: LorenzVec,
    val distance: Double
) : LorenzEvent()