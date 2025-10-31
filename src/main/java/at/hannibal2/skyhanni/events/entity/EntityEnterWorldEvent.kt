package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.Entity

class EntityEnterWorldEvent<T : Entity>(val entity: T) : GenericHanniEvent<T>(entity.javaClass)
