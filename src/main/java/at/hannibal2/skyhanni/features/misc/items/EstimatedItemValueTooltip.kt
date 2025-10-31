package at.hannibal2.hanni.features.misc.items

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.NumberUtil.addSeparators

@HanniModule
object EstimatedItemValueTooltip {

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipEvent) {
        if (!HanniMod.feature.inventory.estimatedItemValues.showTooltip) return
        event.itemStack.getInternalNameOrNull() ?: return

        val total = EstimatedItemValueCalculator.getTotalPrice(event.itemStack, ignoreBasePrice = true) ?: return
        event.toolTip.add("§e§lEstimated Value: §6§l${total.addSeparators()} coins")
    }
}
