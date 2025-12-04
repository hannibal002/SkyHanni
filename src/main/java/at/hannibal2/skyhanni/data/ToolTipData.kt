package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.compat.Text
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.mixins.hooks.renderToolTip
//$$ import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
//$$ import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
//#endif

// Please use ToolTipTextEvent over ToolTipEvent, ItemHoverEvent, ItemHoverEvent is only used for special use cases (e.g. neu pv)
object ToolTipData {

    //#if MC > 1.21
    //$$ init {
    //$$     ItemTooltipCallback.EVENT.register { stack, context, type, originalToolTip ->
    //$$         val slot = lastSlot
    //$$         if (ToolTipTextEvent(slot, stack, originalToolTip).post()) {
    //$$             originalToolTip.clear()
    //$$             return@register
    //$$         }
    //$$     }
    //$$ }
    //$$
    //$$ @JvmStatic
    //$$ fun processModernTooltip(
    //$$     context: DrawContext,
    //$$     stack: ItemStack,
    //$$     originalToolTip: MutableList<Text>,
    //$$ ): MutableList<Text> {
    //$$     val tooltip = originalToolTip.map { it.formattedTextCompatLessResets().removePrefix("§5") }.toMutableList()
    //$$     val tooltipCopy = tooltip.toMutableList()
    //$$     getTooltip(stack, tooltip)
    //$$     onHover(context, stack, tooltip)
    //$$     renderToolTip(context, stack)
    //$$     if (tooltip.isEmpty()) {
    //$$         return mutableListOf()
    //$$     }
    //$$     if (tooltip == tooltipCopy) {
    //$$         return originalToolTip
    //$$     }
    //$$     // TODO need a better way to handle this
    //$$     val newTooltip = mutableListOf<Text>()
    //$$     for ((i, line) in tooltip.withIndex()) {
    //$$         if (tooltipCopy.size > i && tooltipCopy[i] == line) {
    //$$             newTooltip.add(originalToolTip[i])
    //$$         } else {
    //$$             newTooltip.add(Text.of(tooltip[i]))
    //$$         }
    //$$     }
    //$$     return newTooltip
    //$$ }
    //#endif

    @JvmStatic
    fun getTooltip(stack: ItemStack, toolTip: MutableList<String>) {
        val slot = lastSlot ?: return
        val itemStack = slot.stack ?: return
        try {
            if (ToolTipEvent(slot, itemStack, toolTip).post()) {
                toolTip.clear()
            }
            if (PlatformUtils.IS_LEGACY) {
                val textTooltip = toolTip.map { Text.of(it) }.toMutableList()
                if (ToolTipTextEvent(slot, itemStack, textTooltip).post()) {
                    toolTip.clear()
                }
                toolTip.clear()
                toolTip.addAll(textTooltip.map { it.string }.toMutableList())
            }
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e, "Error in item tool tip parsing or rendering detected",
                "toolTip" to toolTip,
                "slot" to slot,
                "slotNumber" to slot.slotNumber,
                "slotIndex" to slot.slotIndex,
                "itemStack" to itemStack,
                "name" to itemStack.displayName,
                "internal name" to itemStack.getInternalName(),
                "lore" to itemStack.getLore(),
            )
        }
    }

    @JvmStatic
    fun onHover(context: DrawContext, stack: ItemStack, toolTip: MutableList<String>) {
        ItemHoverEvent(context, stack, toolTip).post()
    }

    var lastSlot: Slot? = null

}
