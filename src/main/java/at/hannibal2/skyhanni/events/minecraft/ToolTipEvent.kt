package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.Legacy
import net.minecraft.world.inventory.Slot

/**
 * Use [ToolTipTextEvent] Instead
 */
@Legacy("Use ToolTipTextEvent instead", ReplaceWith("ToolTipTextEvent"))
class ToolTipEvent(val slot: Slot, val itemStack: SafeItemStack, private val toolTip0: MutableList<String>) : CancellableSkyHanniEvent() {

    var toolTip: MutableList<String>
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0

    fun toolTipRemovedPrefix() = toolTip.map { it.removePrefix("§5§o") }
}
