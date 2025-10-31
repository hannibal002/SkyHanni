package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.Entity
import net.minecraft.util.ChatComponentText

class EntityDisplayNameEvent<T : Entity>(val entity: T, var chatComponent: ChatComponentText) : GenericHanniEvent<T>(entity.javaClass)
