package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import net.minecraft.entity.Entity

class EntityClickEvent(val clickType: ClickType, val clickedEntity: Entity?) : LorenzEvent()