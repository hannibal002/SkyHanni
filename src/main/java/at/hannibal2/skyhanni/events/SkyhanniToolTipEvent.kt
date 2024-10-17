package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.Cancelable


class SkyhanniToolTipEvent(val slot: Slot, val itemStack: ItemStack, private val toolTip0: MutableList<String>) :
    CancellableSkyHanniEvent() {

    var toolTip: MutableList<String>
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0

    fun toolTipRemovedPrefix() = toolTip.map { it.removePrefix("§5§o") }
}
