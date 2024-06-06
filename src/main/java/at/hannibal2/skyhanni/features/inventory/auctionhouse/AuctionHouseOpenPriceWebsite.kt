package at.hannibal2.skyhanni.features.inventory.auctionhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AuctionHouseOpenPriceWebsite {

    private val config get() = SkyHanniMod.feature.inventory.auctions
    private var lastClick = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("inventory.auctionhouse")

    /**
     * REGEX-TEST: Auctions: "hyperion"
     */
    private val ahSearchPattern by patternGroup.pattern(
        "title.search",
        "Auctions: \"(?<searchTerm>.*)\"?"
    )

    private var searchTerm = ""
    private var displayItem: ItemStack? = null

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        ahSearchPattern.matchMatcher(event.inventoryName) {
            searchTerm = group("searchTerm").removeSuffix("\"").replace(" ", "%20")
            displayItem = createDisplayItem()
        }
    }

    private fun createDisplayItem() = Utils.createItemStack(
        "PAPER".asInternalName().getItemStack().item,
        "§bPrice History",
        "§7Click here to open",
        "§7the price history",
        "§7of §e$searchTerm",
        "§7on §csky.coflnet.com"
    )

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        displayItem = null
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        if (event.inventory is InventoryPlayer) return

        if (event.slot == 8) {
            displayItem?.let {
                event.replace(it)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        displayItem ?: return
        if (event.slotId != 8) return
        event.isCanceled = true
        if (lastClick.passedSince() > 0.3.seconds) {
            val url = "https://sky.coflnet.com/api/mod/open/$searchTerm"
            OSUtils.openBrowser(url)
            lastClick = SimpleTimeMark.now()
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.openPriceWebsite
}
