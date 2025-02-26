package at.hannibal2.skyhanni.events.inventory

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.item.ItemStack

class AnvilUpdateEvent(val left: ItemStack?, val right: ItemStack?) : SkyHanniEvent()
