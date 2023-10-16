package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.borderLine
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI.motesNpcPrice
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.ItemUtils.isVanilla
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.MultiFilter
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRiftExportable
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRiftTransferable
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HideNotClickableItems {
    private val config get() = SkyHanniMod.feature.inventory.hideNotClickable

    private var hideReason = ""
    private var reverseColor = false

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
            event.getConstant("TradeNpcs")?.let {
                tradeNpcFilter.load(it)
            }

            val hideNotClickableItems = event.getConstant("HideNotClickableItems") ?: return
            hideNpcSellFilter.load(hideNotClickableItems["hide_npc_sell"].asJsonObject)
            hideInStorageFilter.load(hideNotClickableItems["hide_in_storage"].asJsonObject)
            updateSalvageList(hideNotClickableItems)
            hidePlayerTradeFilter.load(hideNotClickableItems["hide_player_trade"].asJsonObject)
            notAuctionableFilter.load(hideNotClickableItems["not_auctionable"].asJsonObject)

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("Error in RepositoryReloadEvent for HideNotClickableItems")
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
        if (!LorenzUtils.inSkyBlock) return
        if (isDisabled()) return
        if (bypasssActive()) return
        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.getInventoryName()

        for (slot in chest.inventorySlots) {
            if (slot == null) continue

            if (slot.slotNumber == slot.slotIndex) continue
            if (slot.stack == null) continue

            if (hide(chestName, slot.stack)) {
                val opacity = config.opacity
                val color = LorenzColor.DARK_GRAY.addOpacity(opacity)
                slot.stack.background = color.rgb
            } else if (reverseColor && config.itemsGreenLine) {
                val color = LorenzColor.GREEN.addOpacity(200)
                slot.stack.borderLine = color.rgb
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        if (isDisabled()) return
        if (event.toolTip == null) return
        if (bypasssActive()) return

        val guiChest = Minecraft.getMinecraft().currentScreen
        if (guiChest !is GuiChest) return
        val chestName = (guiChest.inventorySlots as ContainerChest).getInventoryName()

        val stack = event.itemStack
        if (InventoryUtils.getItemsInOpenChest().map { it.stack }.contains(stack)) return
        if (!ItemUtils.getItemsInInventory().contains(stack)) return

        if (hide(chestName, stack)) {
            val first = event.toolTip[0]
            event.toolTip.clear()
            event.toolTip.add("§7" + first.removeColor())
            event.toolTip.add("")
            if (hideReason == "") {
                event.toolTip.add("§4No hide reason!")
                LorenzUtils.warning("No hide reason for not clickable item!")
            } else {
                event.toolTip.add("§c$hideReason")
                if (config.itemsBypass) {
                    event.toolTip.add("  §7(Disable with holding the control key)")
                }
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (isDisabled()) return
        if (!config.itemsBlockClicks) return
        if (bypasssActive()) return
        if (event.gui !is GuiChest) return
        val chestName = InventoryUtils.openInventoryName()

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

    private fun bypasssActive() = config.itemsBypass && KeyboardManager.isControlKeyDown()

    private fun isDisabled(): Boolean {
        if (bypassUntil > System.currentTimeMillis()) return true

        return !config.items
    }

    private fun hide(chestName: String, stack: ItemStack): Boolean {
        hideReason = ""
        reverseColor = false

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
            hideComposter(chestName, stack) -> true
            hideRiftMotesGrubber(chestName, stack) -> true
            hideRiftTransferChest(chestName, stack) -> true
            else -> {
                false
            }
        }
    }

    private fun hideRiftTransferChest(chestName: String, stack: ItemStack): Boolean {
        if (chestName != "Rift Transfer Chest") return false

        reverseColor = true
        val riftTransferable = stack.isRiftTransferable() ?: return true
        if (riftTransferable) {
            return false
        }
        if (RiftAPI.inRift()) {
            val riftExportable = stack.isRiftExportable() ?: return true
            if (riftExportable) {
                return false
            }
        }

        hideReason = "Not Rift-Transferable!"
        return true
    }

    private fun hideRiftMotesGrubber(chestName: String, stack: ItemStack): Boolean {
        if (!RiftAPI.inRift()) return false
        if (chestName != "Motes Grubber") return false

        reverseColor = true

        if (stack.motesNpcPrice() != null) return false

        hideReason = "Not sellable for Motes!"
        return true
    }

    private fun hideComposter(chestName: String, stack: ItemStack): Boolean {
        if (!ComposterOverlay.inInventory) return false

        reverseColor = true

        val internalName = stack.getInternalName_old()
        if (internalName == ComposterOverlay.currentOrganicMatterItem) {
            return false
        }
        if (internalName == ComposterOverlay.currentFuelItem) {
            return false
        }

        hideReason = "Only sell the selected items!"
        return true
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
                reverseColor = true
                return false
            }
        }

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the potion bag!"
            return true
        }

        hideReason = "This item cannot be put into your equipment!"
        return true
    }

    private fun hideAttributeFusion(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Attribute Fusion")) return false

        reverseColor = true

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

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the potion bag!"
            return true
        }

        reverseColor = true
        if (stack.cleanName().endsWith(" Potion")) return false

        hideReason = "This item is not a potion!"
        return true
    }

    private fun hideFishingBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Fishing Bag")) return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the fishing bag!"
            return true
        }

        reverseColor = true
        if (stack.getLore().any { it.removeColor() == "Fishing Bait" }) {
            return false
        }
        hideReason = "This item is not a fishing bait!"
        return true
    }

    private fun hideSackOfSacks(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Sack of Sacks")) return false
        if (ItemUtils.isSkyBlockMenuItem(stack)) return false

        reverseColor = true
        if (ItemUtils.isSack(stack)) return false


        hideReason = "This item is not a sack!"
        return true
    }

    private fun hideAccessoryBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Accessory Bag") && !chestName.startsWith("Accessory Bag (")) return false
        if (ItemUtils.isSkyBlockMenuItem(stack)) return false

        reverseColor = true
        if (stack.getLore().any { it.contains("ACCESSORY") || it.contains("HATCCESSORY") }) return false

        hideReason = "This item is not an accessory!"
        return true
    }

    private fun hidePlayerTrade(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("You    ")) return false

        if (ItemUtils.isCoopSoulBound(stack)) {
            hideReason = "Soulbound items cannot be traded!"
            return true
        }

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be traded!"
            return true
        }

        val name = stack.cleanName()

        if (ItemUtils.isSack(stack)) {
            hideReason = "Sacks cannot be traded!"
            return true
        }

        val result = hidePlayerTradeFilter.match(name)

        if (result) hideReason = "This item cannot be traded!"
        return result
    }

    private fun hideNpcSell(chestName: String, stack: ItemStack): Boolean {
        if (!tradeNpcFilter.match(chestName)) return false
        if (VisitorAPI.inVisitorInventory) return false
        reverseColor = true

        var name = stack.cleanName()
        val size = stack.stackSize
        val amountText = " x$size"
        if (name.endsWith(amountText)) {
            name = name.substring(0, name.length - amountText.length)
        }

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be sold at the NPC!"
            return true
        }

        if (!ItemUtils.isRecombobulated(stack) && LorenzUtils.noTradeMode) {
            if (BazaarApi.isBazaarItem(stack)) {
                return false
            }

            if (hideNpcSellFilter.match(name)) return false

            if (stack.isVanilla() && !stack.isEnchanted()) {
                return false
            }
        }

        hideReason = "This item should not be sold at the NPC!"
        return true
    }

    private fun hideInStorage(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.contains("Ender Chest") && !chestName.contains("Backpack") && chestName != "Storage") return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the storage!"
            return true
        }

        val name = stack.cleanName()

        val result = hideInStorageFilter.match(name)

        if (result) hideReason = "Bags cannot be put into the storage!"
        return result
    }

    private fun hideSalvage(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.equalsOneOf("Salvage Item", "Salvage Items")) return false
        reverseColor = true

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

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
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
        val bazaarInventory = BazaarApi.inBazaarInventory

        val auctionHouseInventory =
            chestName == "Co-op Auction House" || chestName == "Auction House" || chestName == "Create BIN Auction" || chestName == "Create Auction"
        if (!bazaarInventory && !auctionHouseInventory) return false
        reverseColor = true

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            if (bazaarInventory) hideReason = "The SkyBlock Menu is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "The SkyBlock Menu cannot be auctioned!"
            return true
        }

        if (bazaarInventory != BazaarApi.isBazaarItem(stack)) {
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

        if (ItemUtils.isSack(stack)) {
            hideReason = "Sacks cannot be auctioned!"
            return true
        }

        val result = notAuctionableFilter.match(name)
        if (result) hideReason = "This item cannot be auctioned!"
        return result
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "inventory.hideNotClickableItems", "inventory.hideNotClickable.items")
        event.move(3, "inventory.hideNotClickableItemsBlockClicks", "inventory.hideNotClickable.itemsBlockClicks")
        event.move(3, "inventory.hideNotClickableOpacity", "inventory.hideNotClickable.opacity")
        event.move(3, "inventory.notClickableItemsBypass", "inventory.hideNotClickable.itemsBypass")
        event.move(3, "inventory.hideNotClickableItemsGreenLine", "inventory.hideNotClickable.itemsGreenLine")

    }
}
