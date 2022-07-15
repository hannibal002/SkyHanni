package at.hannibal2.skyhanni.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.bazaar.BazaarApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.removeColorCodes
import at.hannibal2.skyhanni.utils.MultiFilter
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
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

    private val hideNpcSellList = MultiFilter()
    private val hideInStorageList = MultiFilter()
    private val tradeNpcList = MultiFilter()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val hideNotClickableItems = event.getConstant("HideNotClickableItems")!!
            hideNpcSellList.load(hideNotClickableItems["hide_npc_sell"].asJsonObject)
            hideInStorageList.load(hideNotClickableItems["hide_in_storage"].asJsonObject)

            val tradeNpcs = event.getConstant("TradeNpcs")!!
            tradeNpcList.load(tradeNpcs)
        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
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

        return !SkyHanniMod.feature.items.hideNotClickableItems
    }

    private fun hide(chestName: String, stack: ItemStack): Boolean {
        hideReason = ""
        return when {
            hideNpcSell(chestName, stack) -> true
            hideInStorage(chestName, stack) -> true
            hideSalvage(chestName, stack) -> true
            hideTrade(chestName, stack) -> true
            hideBazaarOrAH(chestName, stack) -> true
            hideAccessoryBag(chestName, stack) -> true
            hideSackOfSacks(chestName, stack) -> true
            hideFishingBag(chestName, stack) -> true
            hidePotionBag(chestName, stack) -> true

            else -> false
        }
    }

    private fun hidePotionBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Potion Bag")) return false

        val name = stack.cleanName()
        if (isSkyBlockMenuItem(name)) {
            hideReason = "The SkyBlock Menu cannot be put into the potion bag!"
            return true
        }

        if (stack.cleanName().endsWith(" Potion")) return false

        hideReason = "This item is not a potion!"
        return true
    }

    private fun hideFishingBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Fishing Bag")) return false

        val name = stack.cleanName()
        if (isSkyBlockMenuItem(name)) {
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
        if (isSkyBlockMenuItem(name)) return false

        hideReason = "This item is not a sack!"
        return true
    }

    private fun hideAccessoryBag(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("Accessory Bag")) return false

        if (stack.getLore().any { it.contains("ACCESSORY") }) return false
        if (isSkyBlockMenuItem(stack.cleanName())) return false

        hideReason = "This item is not an accessory!"
        return true
    }

    private fun hideTrade(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.startsWith("You    ")) return false

        if (ItemUtils.isSoulBound(stack)) {
            hideReason = "Soulbound items cannot be traded!"
            return true
        }

        val name = stack.cleanName()

        if (ItemUtils.isSack(name)) {
            hideReason = "Sacks cannot be traded!"
            return true
        }

        if (isSkyBlockMenuItem(name)) {
            hideReason = "The SkyBlock Menu cannot be traded!"
            return true
        }

        val result = when {
            name.contains("Personal Deletor") -> true
            name.contains("Day Crystal") -> true
            name.contains("Night Crystal") -> true
            name.contains("Cat Talisman") -> true
            name.contains("Lynx Talisman") -> true
            name.contains("Cheetah Talisman") -> true
            else -> false
        }

        if (result) hideReason = "This item cannot be traded!"
        return result
    }

    private fun hideNpcSell(chestName: String, stack: ItemStack): Boolean {
        if (!tradeNpcList.match(chestName)) return false

        var name = stack.cleanName()
        val size = stack.stackSize
        val amountText = " x$size"
        if (name.endsWith(amountText)) {
            name = name.substring(0, name.length - amountText.length)
        }

        if (isSkyBlockMenuItem(name)) {
            hideReason = "The SkyBlock Menu cannot be sold at the NPC!"
            return true
        }

        if (!ItemUtils.isRecombobulated(stack)) {
            if (hideNpcSellList.match(name)) return false
        }

        hideReason = "This item should not be sold at the NPC!"
        return true
    }

    private fun hideInStorage(chestName: String, stack: ItemStack): Boolean {
        if (!chestName.contains("Ender Chest") && !chestName.contains("Backpack") && chestName != "Storage") return false

        val name = stack.cleanName()

        if (isSkyBlockMenuItem(name)) {
            hideReason = "The SkyBlock Menu cannot be put into the storage!"
            return true
        }
        if (ItemUtils.isSack(name)) {
            hideReason = "Sacks cannot be put into the storage!"
            return true
        }

        val result = hideInStorageList.match(name)

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

        val name = stack.cleanName()

        val armorSets = listOf(
            "Zombie Knight",
            "Heavy",
            "Zombie Soldier",
            "Skeleton Grunt",
            "Skeleton Soldier",
            "Zombie Commander",
            "Skeleton Master",
            "Sniper",
            "Skeletor",
            "Rotten",
        )

        val items = mutableListOf<String>()
        for (armor in armorSets) {
            items.add("$armor Helmet")
            items.add("$armor Chestplate")
            items.add("$armor Leggings")
            items.add("$armor Boots")
        }

        items.add("Zombie Soldier Cutlass")
        items.add("Silent Death")
        items.add("Zombie Knight Sword")
        items.add("Conjuring")
        items.add("Dreadlord Sword")
        items.add("Soulstealer Bow")
        items.add("Machine Gun Bow")
        items.add("Earth Shard")
        items.add("Zombie Commander Whip")
        items.add("Sniper Bow")

        //Crimson essence
        items.add("Blade of the Volcano")
        items.add("Staff of the Volcano")
        items.add("Slug Boots")
        items.add("Flaming Chestplate")
        items.add("Moogma Leggings")
        items.add("Rampart Leggings")
        items.add("Rampart Boots")

        for (item in items) {
            if (name.endsWith(item)) {
                return false
            }
        }

        if (isSkyBlockMenuItem(name)) {
            hideReason = "The SkyBlock Menu cannot be salvaged!"
            return true
        }

        hideReason = "This item cannot be salvaged!"
        return true
    }

    private fun hideBazaarOrAH(chestName: String, stack: ItemStack): Boolean {
        val bazaarInventory = BazaarApi.isBazaarInventory(chestName)

        val auctionHouseInventory =
            chestName == "Co-op Auction House" || chestName == "Auction House" || chestName == "Create BIN Auction" || chestName == "Create Auction"
        if (!bazaarInventory && !auctionHouseInventory) return false


        val displayName = stack.displayName

        if (isSkyBlockMenuItem(displayName.removeColorCodes())) {
            if (bazaarInventory) hideReason = "The SkyBlock Menu is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "The SkyBlock Menu cannot be auctioned!"
            return true
        }

        if (bazaarInventory != BazaarApi.isBazaarItem(displayName)) {
            if (bazaarInventory) hideReason = "This item is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "Bazaar Products cannot be auctioned!"

            return true
        }

        if (isNotAuctionable(stack)) return true

        return false
    }

    private fun isNotAuctionable(stack: ItemStack): Boolean {
        if (ItemUtils.isSoulBound(stack)) {
            hideReason = "Soulbound items cannot be auctioned!"
            return true
        }

        val name = stack.cleanName()

        if (ItemUtils.isSack(name)) {
            hideReason = "Sacks cannot be auctioned!"
            return true
        }

        val result = when {
            name.contains("Personal Deletor") -> true
            name.contains("Day Crystal") -> true
            name.contains("Night Crystal") -> true

            name.contains("Cat Talisman") -> true
            name.contains("Lynx Talisman") -> true
            name.contains("Cheetah Talisman") -> true

            name.contains("Hoe of Great Tilling") -> true
            name.contains("Hoe of Greater Tilling") -> true
            name.contains("InfiniDirt") -> true
            name.contains("Prismapump") -> true
            name.contains("Mathematical Hoe Blueprint") -> true
            name.contains("Basket of Seeds") -> true
            name.contains("Nether Wart Pouch") -> true

            name.contains("Carrot Hoe") -> true
            name.contains("Sugar Cane Hoe") -> true
            name.contains("Nether Warts Hoe") -> true
            name.contains("Potato Hoe") -> true
            name.contains("Melon Dicer") -> true
            name.contains("Pumpkin Dicer") -> true
            name.contains("Coco Chopper") -> true
            name.contains("Wheat Hoe") -> true

            else -> false
        }

        if (result) hideReason = "This item cannot be auctioned!"
        return result
    }

    private fun isSkyBlockMenuItem(name: String): Boolean = name == "SkyBlock Menu (Right Click)"
}
