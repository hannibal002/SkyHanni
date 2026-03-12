package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.data.WorldClickType
import net.minecraft.world.item.ItemStack

open class WorldClickEvent(val itemInHand: ItemStack?, val clickType: WorldClickType) : CancellableSkyHanniEvent()
