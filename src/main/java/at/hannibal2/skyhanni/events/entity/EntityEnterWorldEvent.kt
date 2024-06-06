package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.events.LorenzEvent
import net.minecraft.entity.Entity

class EntityEnterWorldEvent(val entity: Entity) : LorenzEvent()
