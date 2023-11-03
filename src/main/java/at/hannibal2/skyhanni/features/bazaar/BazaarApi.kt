package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BazaarOpenedProductEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarApi {
    private var loadedNpcPriceData = false

    companion object {
        val holder = BazaarDataHolder()
        var inBazaarInventory = false
        private var currentSearchedItem = ""

        var currentlyOpenedProduct: NEUInternalName? = null

        fun getBazaarDataByName(name: String): BazaarData? = NEUItems.getInternalNameOrNull(name)?.getBazaarData()

        fun NEUInternalName.getBazaarData() = if (isBazaarItem()) {
            holder.getData(this)
        } else null

        fun isBazaarItem(stack: ItemStack) = stack.getInternalName().isBazaarItem()

        fun NEUInternalName.isBazaarItem() = NEUItems.manager.auctionManager.getBazaarInfo(asString()) != null

        fun searchForBazaarItem(displayName: String, amount: Int = -1) {
            if (!LorenzUtils.inSkyBlock) return
            if (NEUItems.neuHasFocus()) return
            if (LorenzUtils.noTradeMode) return
            if (LorenzUtils.inDungeons || LorenzUtils.inKuudraFight) return
            LorenzUtils.sendCommandToServer("bz ${displayName.removeColor()}")
            if (amount != -1) OSUtils.copyToClipboard(amount.toString())
            currentSearchedItem = displayName.removeColor()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inBazaarInventory = checkIfInBazaar(event)
        if (!inBazaarInventory) return

        val openedProduct = getOpenedProduct(event.inventoryItems);
        if (openedProduct != null) {
            currentlyOpenedProduct = openedProduct
            BazaarOpenedProductEvent(openedProduct, event).postAndCatch()
        }
    }

    private fun getOpenedProduct(inventoryItems: Map<Int, ItemStack>): NEUInternalName? {
        val buyInstantly = inventoryItems[10] ?: return null

        if (buyInstantly.displayName != "§aBuy Instantly") return null
        val bazaarItem = inventoryItems[13] ?: return null

        val itemName = bazaarItem.displayName
        return NEUItems.getInternalNameOrNull(itemName)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {

        if (!loadedNpcPriceData) {
            loadedNpcPriceData = true
            holder.start()
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inBazaarInventory) return
        if (!SkyHanniMod.feature.bazaar.purchaseHelper) return
        if (currentSearchedItem == "") return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            val stack = slot.stack ?: continue

            if (chest.inventorySlots.indexOf(slot) !in 9..44) {
                continue
            }

            if (stack.displayName.removeColor() == currentSearchedItem) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if ("\\[Bazaar] (Buy Order Setup!|Bought).*$currentSearchedItem.*".toRegex()
                .matches(event.message.removeColor())
        ) {
            currentSearchedItem = ""
        }
    }

    private fun checkIfInBazaar(event: InventoryFullyOpenedEvent): Boolean {
        val returnItemSlot = intArrayOf(event.inventorySize - 5, event.inventorySize - 6)
        for (slot in returnItemSlot) {
            var item = event.inventoryItems.get(slot)
            if (item == null || !item.name?.removeColor().let { it == "Go Back" }
                || !item.getLore().get(0)?.removeColor().let { it == "To Bazaar" })
                continue
            return true
        }

        if (event.inventoryName.startsWith("Bazaar ➜ ")) return true
        return when (event.inventoryName) {
            "How many do you want?" -> true
            "How much do you want to pay?" -> true
            "Confirm Buy Order" -> true
            "Confirm Instant Buy" -> true
            "At what price are you selling?" -> true
            "Confirm Sell Offer" -> true
            "Order options" -> true

            else -> false
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inBazaarInventory = false
        currentlyOpenedProduct = null
    }
}
