package at.hannibal2.skyhanni.events

import net.minecraft.entity.Entity

data class EntityCustomNameUpdateEvent(
    val newName: String?,
    val entity: Entity,
) : LorenzEvent()
