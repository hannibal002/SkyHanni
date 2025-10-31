package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.Entity

data class EntityCustomNameUpdateEvent<T : Entity>(
    val entity: T,
    val newName: String?,
) : GenericHanniEvent<T>(entity.javaClass)
