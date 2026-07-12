package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.world.entity.Entity

@Thread(RENDER)
data class EntityCustomNameUpdateEvent<T : Entity>(
    val entity: T,
    val newName: String?,
) : GenericSkyHanniEvent<T>(entity.javaClass)
