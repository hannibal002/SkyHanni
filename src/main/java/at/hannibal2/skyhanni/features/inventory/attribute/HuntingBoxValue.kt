package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

@SkyHanniModule
object HuntingBoxValue {

    private val config get() = AttributeShardsData.config
    private var display = emptyList<Renderable>()

    fun processInventory(slots: List<Slot>) {
        if (!config.huntingBoxValue) return

        val table = mutableListOf<DisplayTableEntry>()

        for (slot in slots) {
            val slotNumber = slot.slotNumber
            if (!isValidSlotNumber(slotNumber)) continue
            val stack = slot.stack.orNull() ?: continue
            processAttributeShardSlot(slotNumber, stack, table)
        }

        val newList = mutableListOf<Renderable>()
        newList.add(StringRenderable("§eHunting Box Value"))
        newList.add(RenderableUtils.fillTable(table, padding = 5, itemScale = 0.7))
        display = newList
    }

    private fun processAttributeShardSlot(slotNumber: Int, stack: ItemStack, table: MutableList<DisplayTableEntry>) {
        val internalName = stack.getInternalNameOrNull() ?: return

        val amountOwned = AttributeShardsData.amountOwnedPattern.firstMatcher(stack.getLore()) {
            group("amount").formatInt()
        } ?: return

        val pricePerInstantSell = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_SELL)
        val totalPriceInstantSell = pricePerInstantSell * amountOwned

        val pricePerInstantBuy = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_BUY)
        val totalPriceInstantBuy = pricePerInstantBuy * amountOwned

        val hover = buildList {
            add(internalName.repoItemName)
            add("")
            add("§7Price per Instant Sell: §6${pricePerInstantSell.toInt().addSeparators()}")
            add("§7Price per Instant Buy: §6${pricePerInstantBuy.toInt().addSeparators()}")
            add("")
            add("§7Amount Owned: §a$amountOwned")
            add("§7Total Price Instant Sell: §6${totalPriceInstantSell.toInt().addSeparators()}")
            add("§7Total Price Instant Buy: §6${totalPriceInstantBuy.toInt().addSeparators()}")
        }

        table.add(
            DisplayTableEntry(
                "${internalName.repoItemName} §8x$amountOwned",
                "§6${totalPriceInstantSell.toInt().addSeparators()}",
                totalPriceInstantSell,
                internalName,
                hover,
                highlightsOnHoverSlots = listOf(slotNumber),
            ),
        )
    }

    private fun isValidSlotNumber(slot: Int): Boolean {
        if (slot < 9 || slot > 44) return false
        val modNine = slot % 9
        return modNine != 0 && modNine != 8
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onRenderOverlay() {
        if (!config.huntingBoxValue) return
        if (!AttributeShardsData.huntingBoxInventory.isInside()) return

        config.huntingBoxValuePosition.renderRenderables(display, posLabel = "Hunting Box Value")
    }
}
