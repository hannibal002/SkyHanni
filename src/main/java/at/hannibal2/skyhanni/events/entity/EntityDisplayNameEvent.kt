package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.entity.Entity
import net.minecraft.text.Text

class EntityDisplayNameEvent<T : Entity>(val entity: T, var chatComponent: Text) : GenericSkyHanniEvent<T>(entity.javaClass)
