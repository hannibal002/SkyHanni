package at.hannibal2.hanni.events.inventory

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.item.ItemStack

class AnvilUpdateEvent(val left: ItemStack?, val right: ItemStack?) : HanniEvent()
