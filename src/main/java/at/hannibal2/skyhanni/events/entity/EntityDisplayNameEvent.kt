package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

class EntityDisplayNameEvent<T : Entity>(val entity: T, var chatComponent: Component) : GenericSkyHanniEvent<T>(entity.javaClass)
