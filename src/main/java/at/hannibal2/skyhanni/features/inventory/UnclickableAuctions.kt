package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isPet
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetItem
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class UnclickableAuctions {

    private val config get() = SkyHanniMod.feature.inventory.unclickableAuctions

    private val auctionViewChestPattern by RepoPattern.pattern(
        "misc.auctionview.chestname",
        "(?:\\S+ )?Auction View"
    )

    private val auctionButtonNames = listOf<String>("Close", "Bid History", "Go Back")

    private var itemIsRecombOrBoosted = false

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (isBypassActive()) return
        if (!auctionViewChestPattern.matches(InventoryUtils.openInventoryName())) return
        if (!itemIsRecombOrBoosted) return
        val itemStack = event.itemStack ?: return
        val itemName = itemStack.cleanName()
        if (itemName in auctionButtonNames) return
        val (itemType, hideReason) = if (isPet(itemName)) Pair("pet", "tier-boosted") else Pair("item", "recombobulated")
        event.toolTip.clear()
        event.toolTip.addAll(listOf("§7${itemName.removeColor()}", "§cThe $itemType up for auction is hidden as it is $hideReason."))
        if (config.bypassKey != Keyboard.KEY_NONE) event.toolTip.add("§8(To bypass this, please press ahd hold the ${Keyboard.getKeyName(config.bypassKey)} key.)")
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (isBypassActive()) return
        if (!auctionViewChestPattern.matches(InventoryUtils.openInventoryName())) return
        if (!itemIsRecombOrBoosted) return
        val itemName = event.slot?.stack?.cleanName() ?: return
        if (itemName in auctionButtonNames) return
        event.isCanceled = true
        return
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (isBypassActive()) return
        if (!auctionViewChestPattern.matches(event.inventoryName)) {
            itemIsRecombOrBoosted = false
            return
        }
        val itemForAuction = event.inventoryItems[13] ?: return
        if (itemForAuction.isTierBoostedPet() || itemForAuction.isRecombobulated()) itemIsRecombOrBoosted = true
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (isBypassActive()) return
        val guiChest = event.gui
        if (guiChest !is GuiChest) return
        if (!auctionViewChestPattern.matches(InventoryUtils.openInventoryName())) return
        if (!itemIsRecombOrBoosted) return
        val chest = event.gui.inventorySlots as ContainerChest
        val slot = chest.inventorySlots[13] ?: return
        val color = LorenzColor.DARK_GRAY.addOpacity(config.opacity)
        slot.stack.background = color.rgb
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!isEnabled()) return
        itemIsRecombOrBoosted = false
    }

    private fun ItemStack.isTierBoostedPet(): Boolean = isPet(this.cleanName()) && this.getPetItem() == "PET_ITEM_TIER_BOOST"
    private fun isEnabled(): Boolean = LorenzUtils.inSkyBlock && config.enabled
    private fun isBypassActive(): Boolean = config.bypassKey.isKeyHeld()
}
