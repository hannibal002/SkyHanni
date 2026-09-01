package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.Entity

@Deprecated("Use EntityLeaveWorldEvent instead", ReplaceWith("EntityLeaveWorldEvent"))
typealias EntityRemovedEvent<T> = EntityLeaveWorldEvent<T>

@PrimaryFunction("onEntityLeaveWorld")
class EntityLeaveWorldEvent<T : Entity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
