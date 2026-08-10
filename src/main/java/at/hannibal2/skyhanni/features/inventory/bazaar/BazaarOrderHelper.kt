package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarOrdersLoadedEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarDataOrError
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu

@SkyHanniModule
object BazaarOrderHelper {
    private val config get() = SkyHanniMod.feature.inventory.bazaar

    private var highlightedSlots = mapOf<Int, LorenzColor>()

    @HandleEvent
    private fun onBazaarOrdersLoaded(event: BazaarOrdersLoadedEvent) {
        highlightedSlots = if (config.orderHelper) load(event.orders) else emptyMap()
    }

    private fun load(orders: List<BazaarOrder>): Map<Int, LorenzColor> {
        val slots = mutableMapOf<Int, LorenzColor>()
        val errorItems = mutableSetOf<NeuInternalName>()
        for (order in orders) {
            val data = order.internalName.getBazaarData()
            if (data == null) {
                errorItems.add(order.internalName)
                continue
            }
            highlightColor(order, data)?.let { slots[order.slot] = it }
        }
        errorItems.firstOrNull()?.getBazaarDataOrError()

        return slots
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.orderHelper) return
        if (!BazaarOrderApi.inOrderInventory()) return
        if (event.gui !is ContainerScreen) return
        val chest = event.container as ChestMenu
        for ((slot, _) in chest.getUpperItems()) {
            highlightedSlots[slot.index]?.let {
                slot.highlight(it)
            }
        }
    }

    private fun highlightColor(order: BazaarOrder, data: BazaarData): LorenzColor? {
        if (order.filled >= order.amount) return LorenzColor.GREEN
        val badPrice = when (order.type) {
            BazaarApi.SimpleTransactionType.BUY_ORDER -> order.pricePerUnit < data.instantSellPrice
            BazaarApi.SimpleTransactionType.SELL_OFFER -> order.pricePerUnit > data.instantBuyPrice
        }
        return if (badPrice) LorenzColor.GOLD else null
    }
}
