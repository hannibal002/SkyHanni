package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

@SkyHanniModule
object EstimatedItemValueTooltip {

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ItemHoverEvent) {
        if (!SkyHanniMod.feature.inventory.estimatedItemValues.showTooltip) return
        val internalName = event.itemStack.getInternalNameOrNull() ?: return

        val (total, _) = EstimatedItemValueCalculator.calculate(event.itemStack, mutableListOf())
        if (total == 0.0) return
        if (internalName.getPrice() == total) return
        event.toolTip.add("§e§lEstimated Value: §6§l${total.addSeparators()} coins")
    }
}
