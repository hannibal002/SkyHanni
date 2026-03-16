package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.Entity

@PrimaryFunction("onEntityChangeName")
data class EntityCustomNameUpdateEvent<T : Entity>(
    override val entity: T,
    val newName: String?,
) : GenericSkyHanniEvent<T>(entity.javaClass), SkyHanniEntityEvent<T>
