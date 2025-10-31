package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.nextAfter

@HanniModule
object PowerStoneGuideFeatures {

    private val missing = mutableMapOf<Int, NeuInternalName>()
    private var inInventory = false

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Power Stones Guide") return

        inInventory = true

        for ((slot, item) in event.inventoryItems) {
            val lore = item.getLore()
            if (lore.contains("§7Learned: §cNot Yet ✖")) {
                val rawName = lore.nextAfter("§7Power stone:") ?: continue
                val name = NeuInternalName.fromItemName(rawName)
                missing[slot] = name
            }
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        event.container.inventorySlots
            .filter { missing.containsKey(it.slotNumber) }
            .forEach { it.highlight(LorenzColor.RED) }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        val internalName = missing[event.slotId] ?: return

        BazaarApi.searchForBazaarItem(internalName, 9)
    }

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        val internalName = missing[event.slot.slotNumber] ?: return
        val totalPrice = internalName.getPrice() * 9
        event.toolTip.add(5, "9x from Bazaar: §6${totalPrice.shortFormat()}")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && HanniMod.feature.inventory.skyblockGuide.powerStone
}
