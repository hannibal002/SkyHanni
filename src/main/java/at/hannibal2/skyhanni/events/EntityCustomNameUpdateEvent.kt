package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.entity.Entity

data class EntityCustomNameUpdateEvent(
    val newName: String?,
    val entity: Entity,
) : SkyHanniEvent()
