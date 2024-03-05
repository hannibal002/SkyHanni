package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AuctionHouseCopyUnderbidPrice {

    private val config get() = SkyHanniMod.feature.inventory.auctions

    private val patternGroup = RepoPattern.group("auctions.underbid")
    private val auctionPricePattern by patternGroup.pattern(
        "price",
        "§7(?:Buy it now|Starting bid|Top bid): §6(?<coins>[0-9,]+) coins"
    )
    private val allowedInventoriesPattern by patternGroup.pattern(
        "allowedinventories",
        "(?:Auctions Browser|Manage Auctions|Auctions: \".*\"?)"
    )

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.autoCopyUnderbidPrice) return
        if (!event.fullyOpenedOnce) return
        if (event.inventoryName != "Create BIN Auction") return
        val item = event.inventoryItems[13] ?: return

        val internalName = item.getInternalName()
        if (internalName == NEUInternalName.NONE) return

        val price = internalName.getPrice().toLong()
        if (price <= 0) {
            OSUtils.copyToClipboard("")
            return
        }
        val newPrice = price * item.stackSize - 1
        OSUtils.copyToClipboard("$newPrice")
        ChatUtils.chat("Copied ${newPrice.addSeparators()} to clipboard. (Copy Underbid Price)")
    }

    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!config.copyUnderbidKeybind.isKeyHeld()) return
        if (!LorenzUtils.inSkyBlock) return
        if (!allowedInventoriesPattern.matches(InventoryUtils.openInventoryName())) return

        val gui = event.gui as? GuiContainer ?: return
        val stack = gui.slotUnderMouse?.stack ?: return
        val lore = stack.getLore()

        for (line in lore) {
            auctionPricePattern.matchMatcher(line) {
                val underbid = group("coins").formatNumber() - 1
                OSUtils.copyToClipboard("$underbid")
                ChatUtils.chat("Copied ${underbid.addSeparators()} to clipboard.")
                return
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(25, "inventory.copyUnderbidPrice", "inventory.auctions.autoCopyUnderbidPrice")
    }
}
