package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EstimatedWardrobePrice {
    private val config get() = SkyHanniMod.feature.misc.estimatedItemValues
    var data = mutableMapOf<Int, MutableList<ItemStack>>()

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.armor) return
        if (!InventoryUtils.openInventoryName().contains("Wardrobe")) return

        val slot = event.slot.slotNumber
        val id = slot % 9
        // Only showing in the armor select line
        if (slot - id != 36) return
        val items = data[id] ?: return

        var index = 3
        val toolTip = event.toolTip
        if (toolTip.size < 4) return
        toolTip.add(index++, "")
        toolTip.add(index++, "§aEstimated Armor Value:")

        var totalPrice = 0.0
        for (item in items) {
            val name = item.name
            val price = EstimatedItemValueCalculator.calculate(item, mutableListOf()).first
            totalPrice += price

            toolTip.add(index++, "  §7- $name: §6${NumberUtil.format(price)}")
        }
        toolTip.add(index, " §aTotal Value: §6§l${NumberUtil.format(totalPrice)} coins")
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.armor) return
        if (!event.inventoryName.startsWith("Wardrobe")) return

        val map = mutableMapOf<Int, MutableList<ItemStack>>()

        for ((slot, item) in event.inventoryItems) {
            item.getInternalNameOrNull() ?: continue
            val price = EstimatedItemValueCalculator.calculate(item, mutableListOf()).first
            if (price == 0.0) continue
            val id = slot % 9
            val list = map.getOrPut(id) { mutableListOf() }
            list.add(item)
        }
        data = map
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.estimatedIemValueArmor", "misc.estimatedItemValues.armor")
    }
}
