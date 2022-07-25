package at.hannibal2.skyhanni.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.bazaar.BazaarApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSBItemID
import at.hannibal2.skyhanni.utils.LorenzUtils.removeColorCodes
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

class HideNotClickableItems {

    private var hideReason = ""

    private var lastClickTime = 0L
    private var bypassUntil = 0L

    private val hideNpcSellFilter = MultiFilter()
    private val hideInStorageFilter = MultiFilter()
    private val tradeNpcFilter = MultiFilter()
    private val itemsToSalvage = mutableListOf<String>()
    private val hidePlayerTradeFilter = MultiFilter()
    private val notAuctionableFilter = MultiFilter()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val hideNotClickableItems = event.getConstant("HideNotClickableItems")!!
            hideNpcSellFilter.load(hideNotClickableItems["hide_npc_sell"].asJsonObject)
            hideInStorageFilter.load(hideNotClickableItems["hide_in_storage"].asJsonObject)
            tradeNpcFilter.load(event.getConstant("TradeNpcs")!!)
            updateSalvageList(hideNotClickableItems)
            hidePlayerTradeFilter.load(hideNotClickableItems["hide_player_trade"].asJsonObject)
            notAuctionableFilter.load(hideNotClickableItems["not_auctionable"].asJsonObject)

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    private fun updateSalvageList(hideNotClickableItems: JsonObject) {
        itemsToSalvage.clear()
        val salvage = hideNotClickableItems["salvage"].asJsonObject
        itemsToSalvage.addAll(salvage.asJsonObject["items"].asJsonArray.map { it.asString })
        for (armor in salvage.asJsonObject["armor"].asJsonArray.map { it.asString }) {
            itemsToSalvage.add("$armor Helmet")
            itemsToSalvage.add("$armor Chestplate")
            itemsToSalvage.add("$armor Leggings")
            itemsToSalvage.add("$armor Boots")
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (isDisabled()) return
        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        val lightingState = GL11.glIsEnabled(GL11.GL_LIGHTING)
        GlStateManager.disableLighting()
        GlStateManager.color(1f, 1f, 1f, 1f)

        for (slot in chest.inventorySlots) {
            if (slot == null) continue

            if (slot.slotNumber == slot.slotIndex) continue
            if (slot.stack == null) continue

            if (hide(chestName, slot.stack)) {
                slot highlight LorenzColor.GRAY
            }
        }

        if (lightingState) GlStateManager.enableLighting()
    }

    @SubscribeEvent
    fun onDrawSlot(event: GuiContainerEvent.DrawSlotEvent.Pre) {
        //test who is this?
        if (isDisabled()) return
        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        val slot = event.slot
        if (slot.slotNumber == slot.slotIndex) return
        if (slot.stack == null) return

        val stack = slot.stack

        if (hide(chestName, stack)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        if (isDisabled()) return
        if (event.toolTip == null) return
        val guiChest = Minecraft.getMinecraft().currentScreen
        if (guiChest !is GuiChest) return
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        val stack = event.itemStack
        if (ItemUtils.getItemsInOpenChest().contains(stack)) return
        if (!ItemUtils.getItemsInInventory().contains(stack)) return

        if (hide(chestName, stack)) {
            val first = event.toolTip[0]
            event.toolTip.clear()
            event.toolTip.add("§7" + first.removeColorCodes())
            event.toolTip.add("")
            if (hideReason == "") {
                event.toolTip.add("§4No hide reason!")
                LorenzUtils.warning("Not hide reason for not clickable item!")
            } else {
                event.toolTip.add("§c$hideReason")
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (isDisabled()) return
        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return
        if (slot.stack == null) return

        val stack = slot.stack

        if (hide(chestName, stack)) {
            event.isCanceled = true

            if (System.currentTimeMillis() > lastClickTime + 5_000) {
                lastClickTime = System.currentTimeMillis()
            }
            return
        }
    }

    private fun isDisabled(): Boolean {
        if (bypassUntil > System.currentTimeMillis()) return true

        return !SkyHanniMod.feature.inventory.hideNotClickableItems
    }

    private fun hide(chestName: String, stack: ItemStack): Boolean {
        hideReason = ""
        return when {
            hideNpcSell(chestName, stack) -> true
            hideInStorage(chestName, stack) -> true
            hideSalvage(chestName, stack) -> true
            hidePlayerTrade(chestName, stack) -> true
            hideBazaarOrAH(chestName, stack) -> true
            hideAccessoryBag(chestName, stack) -> true
            hideSackOfSacks(chestName, stack) -> true
            hideFishingBag(chestName, stack) -> true
            hidePotionBag(chestName, stack) -> true
            hidePrivateIslandChest(chestName, stack) -> true
            hideAttributeFusion(chestName, stack) -> true
            hideYourEquipment(chestName, stack) -> true
            else -> false
        }
    }

    private fun hideYourEquipment(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Your Equipment")) return false

        val list = listOf(
            "HELMET",
            "CHESTPLATE",
            "LEGGINGS",
            "BOOTS",

            "NECKLACE",
            "CLOAK",
            "BELT",
            "GLOVES",
            "BRACELET"
        )
        for (type in list) {
            if (stack.getLore().any { it.contains("§l") && it.contains(type) }) {//todo use item api
                return false
            }
        }

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the potion bag!"
            return true
        }

        hideReason = "This item cannot be put into your equipment!"
        return true
    }

    private fun hideAttributeFusion(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Attribute Fusion")) return false

        if (ItemUtils.hasAttributes(stack)) return false

        hideReason = "This item has no attributes!"
        return true
    }

    private fun hidePrivateIslandChest(chestName: String, stack: ItemStack): Boolean {
        if (chestName != "Chest" && chestName != "Large Chest") return false
        //TODO make check if player is on private island

        if (!ItemUtils.isSoulBound(stack)) return false

        hideReason = "This item cannot be stored into a chest!"
        return true
    }

    private fun hidePotionBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Potion Bag")) return false

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the potion bag!"
            return true
        }

        if (stack.cleanName().endsWith(" Potion")) return false

        hideReason = "This item is not a potion!"
        return true
    }

    private fun hideFishingBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Fishing Bag")) return false

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the fishing bag!"
            return true
        }

        if (stack.getLore().any { it.removeColorCodes() == "Fishing Bait" }) {
            return false
        }
        hideReason = "This item is not a fishing bait!"
        return true
    }

    private fun hideSackOfSacks(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Sack of Sacks")) return false

        val name = stack.cleanName()
        if (ItemUtils.isSack(name)) return false
        if (isSkyBlockMenuItem(stack)) return false

        hideReason = "This item is not a sack!"
        return true
    }

    private fun hideAccessoryBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Accessory Bag")) return false

        if (stack.getLore().any { it.contains("ACCESSORY") }) return false
        if (isSkyBlockMenuItem(stack)) return false

        hideReason = "This item is not an accessory!"
        return true
    }

    private fun hidePlayerTrade(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("You    ")) return false

        if (ItemUtils.isCoopSoulBound(stack)) {
            hideReason = "Soulbound items cannot be traded!"
            return true
        }

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be traded!"
            return true
        }

        val name = stack.cleanName()

        if (ItemUtils.isSack(name)) {
            hideReason = "Sacks cannot be traded!"
            return true
        }

        val result = hidePlayerTradeFilter.match(name)
        LorenzDebug.log("hidePlayerTradeList filter result for '$name': $result")

        if (result) hideReason = "This item cannot be traded!"
        return result
    }

    private fun hideNpcSell(chestName: String, stack: ItemStack): Boolean {
        if (!tradeNpcFilter.match(chestName)) return false

        var name = stack.cleanName()
        val size = stack.stackSize
        val amountText = " x$size"
        if (name.endsWith(amountText)) {
            name = name.substring(0, name.length - amountText.length)
        }

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be sold at the NPC!"
            return true
        }

        if (!ItemUtils.isRecombobulated(stack)) {
            if (hideNpcSellFilter.match(name)) return false
        }

        hideReason = "This item should not be sold at the NPC!"
        return true
    }

    private fun hideInStorage(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.contains("Ender Chest") && !chestName.contains("Backpack") && chestName != "Storage") return false

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the storage!"
            return true
        }

        val name = stack.cleanName()

        if (ItemUtils.isSack(name)) {
            hideReason = "Sacks cannot be put into the storage!"
            return true
        }

        val result = hideInStorageFilter.match(name)

        if (result) hideReason = "Bags cannot be put into the storage!"
        return result
    }

    private fun hideSalvage(chestName: String, stack: ItemStack): Boolean {
        if (chestName != "Salvage Item") return false

        if (ItemUtils.isRecombobulated(stack)) {
            hideReason = "This item should not be salvaged! (Recombobulated)"
            return true
        }
        for (line in stack.getLore()) {
            if (line.contains("LEGENDARY DUNGEON")) {
                hideReason = "This item should not be salvaged! (Legendary)"
                return true
            }
        }

        if (isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be salvaged!"
            return true
        }

        val name = stack.cleanName()
        for (item in itemsToSalvage) {
            if (name.endsWith(item)) {
                return false
            }
        }

        hideReason = "This item cannot be salvaged!"
        return true
    }

    private fun hideBazaarOrAH(chestName: String, stack: ItemStack): Boolean {
        val bazaarInventory = BazaarApi.isBazaarInventory(chestName)

        val auctionHouseInventory =
            chestName == "Co-op Auction House" || chestName == "Auction House" || chestName == "Create BIN Auction" || chestName == "Create Auction"
        if (!bazaarInventory && !auctionHouseInventory) return false



        if (isSkyBlockMenuItem(stack)) {
            if (bazaarInventory) hideReason = "The SkyBlock Menu is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "The SkyBlock Menu cannot be auctioned!"
            return true
        }

        val displayName = stack.displayName
        if (bazaarInventory != BazaarApi.isBazaarItem(displayName)) {
            if (bazaarInventory) hideReason = "This item is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "Bazaar Products cannot be auctioned!"

            return true
        }

        if (isNotAuctionable(stack)) return true

        return false
    }

    private fun isNotAuctionable(stack: ItemStack): Boolean {
        if (ItemUtils.isCoopSoulBound(stack)) {
            hideReason = "Soulbound items cannot be auctioned!"
            return true
        }

        val name = stack.cleanName()

        if (ItemUtils.isSack(name)) {
            hideReason = "Sacks cannot be auctioned!"
            return true
        }

        val result = notAuctionableFilter.match(name)
        if (result) hideReason = "This item cannot be auctioned!"
        return result
    }

    private fun isSkyBlockMenuItem(stack: ItemStack): Boolean = stack.getSBItemID() == "SKYBLOCK_MENU"
}
