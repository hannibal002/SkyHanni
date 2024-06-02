package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.events.LorenzEvent
import net.minecraft.entity.Entity
import net.minecraft.util.IChatComponent

class EntityDisplayNameEvent(val entity: Entity, var chatComponent: IChatComponent) : LorenzEvent()
