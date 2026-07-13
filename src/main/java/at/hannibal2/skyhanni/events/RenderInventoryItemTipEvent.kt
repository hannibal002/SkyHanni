package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.inventory.Slot

class RenderInventoryItemTipEvent(
    val inventoryName: String,
    val slot: Slot,
    val stack: SafeItemStack,
    var stackTip: String = "",
    var offsetX: Int = 0,
    var offsetY: Int = 0,
    var alignLeft: Boolean = true,
) : SkyHanniEvent()
