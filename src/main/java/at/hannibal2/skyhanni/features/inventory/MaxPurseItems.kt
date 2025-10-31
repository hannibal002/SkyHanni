package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.PurseApi
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatDouble
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderStrings
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object MaxPurseItems {
    private val config get() = HanniMod.feature.inventory.bazaar

    private val patternGroup = RepoPattern.group("inventory.maxpurse")
    private val orderPattern by patternGroup.pattern(
        "order",
        ".*§6(?<coins>[\\d.,]+) coins §7each.*",
    )
    private val instantPattern by patternGroup.pattern(
        "instant",
        ".*Price per unit: §6(?<coins>[\\d.,]+) coins.*",
    )
    private val createOrderPattern by patternGroup.pattern(
        "createorder",
        "§aCreate Buy Order",
    )
    private val createInstantPattern by patternGroup.pattern(
        "createinstant",
        "§aBuy Instantly",
    )

    private var buyOrderPrice: Double? = null
    private var instantBuyPrice: Double? = null

    private fun getPrices() {
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val item = slot.stack
            val name = item.displayName ?: continue
            createOrderPattern.matchMatcher(name) {
                orderPattern.firstMatcher(item.getLore()) {
                    // +0.1 because I expect people to use the gold nugget option
                    buyOrderPrice = group("coins").formatDouble() + 0.1
                    // If we get to this point, we have the instant price because instant is earlier in the list of items
                    // So we can return
                    return
                }
            }
            createInstantPattern.matchMatcher(name) {
                instantPattern.firstMatcher(item.getLore()) {
                    instantBuyPrice = group("coins").formatDouble()
                }
            }
        }
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!BazaarApi.inBazaarInventory) return
        // I would use BazaarAPI for price info, but as soon as NEU's data goes out of date, it will be wrong
        if (BazaarApi.currentlyOpenedProduct == null) {
            buyOrderPrice = null
            instantBuyPrice = null
            return
        }
        if (buyOrderPrice == null && instantBuyPrice == null) {
            getPrices()
        }

        val currentPurse = PurseApi.getPurse()
        val buyOrders = buyOrderPrice?.let {
            (currentPurse / it).toInt()
        } ?: 0
        val buyInstant = instantBuyPrice?.let {
            (currentPurse / it).toInt()
        } ?: 0

        config.maxPurseItemsPosition.renderStrings(
            listOf(
                "§7Max items with purse",
                "§7Buy order +0.1: §e${buyOrders.addSeparators()}x",
                "§7Instant buy: §e${buyInstant.addSeparators()}x",
            ),
            posLabel = "Max Items With Purse",
        )
    }

    fun isEnabled(): Boolean {
        return SkyBlockUtils.inSkyBlock && config.maxPurseItems
    }
}
