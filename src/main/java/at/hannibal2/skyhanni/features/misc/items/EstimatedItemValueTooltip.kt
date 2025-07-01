package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

@SkyHanniModule
object EstimatedItemValueTooltip {

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipEvent) {
        if (!SkyHanniMod.feature.inventory.estimatedItemValues.showTooltip) return
        event.itemStack.getInternalNameOrNull() ?: return

        val total = EstimatedItemValueCalculator.getTotalPrice(event.itemStack, ignoreBasePrice = true) ?: return
        event.toolTip.add("§e§lEstimated Value: §6§l${total.addSeparators()} coins")
    }
}
