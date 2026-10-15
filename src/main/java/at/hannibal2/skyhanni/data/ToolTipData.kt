package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.RenderItemTooltipEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot

// Please use ToolTipTextEvent over ToolTipEvent, ItemHoverEvent, ItemHoverEvent is only used for special use cases (e.g. neu pv)
object ToolTipData {

    init {
        ItemTooltipCallback.EVENT.register { stack, _, _, originalToolTip ->
            val slot = lastSlot
            if (ToolTipTextEvent(slot, stack, originalToolTip).post().isCancelled) {
                originalToolTip.clear()
                return@register
            }
        }
    }

    @JvmStatic
    fun processModernTooltip(
        context: GuiGraphicsExtractor,
        stack: SafeItemStack,
        originalToolTip: MutableList<Component>,
    ): MutableList<Component> {
        val tooltip = originalToolTip.map { it.formattedTextCompatLessResets().removePrefix("§5") }.toMutableList()
        val tooltipCopy = tooltip.toMutableList()
        RenderItemTooltipEvent(context, stack).post()
        if (tooltip.isEmpty()) {
            return mutableListOf()
        }
        if (tooltip == tooltipCopy) {
            return originalToolTip
        }
        // TODO need a better way to handle this
        val newTooltip = mutableListOf<Component>()
        for ((i, line) in tooltip.withIndex()) {
            if (tooltipCopy.size > i && tooltipCopy[i] == line) {
                newTooltip.add(originalToolTip[i])
            } else {
                newTooltip.add(Component.literal(tooltip[i]))
            }
        }
        return newTooltip
    }

    var lastSlot: Slot? = null

}
