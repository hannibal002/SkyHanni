package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import at.hannibal2.hanni.data.ClickType
import net.minecraft.item.ItemStack

open class WorldClickEvent(val itemInHand: ItemStack?, val clickType: ClickType) : CancellableHanniEvent()
