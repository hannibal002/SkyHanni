package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MaxPurseItems {

    private val patternGroup = RepoPattern.group("inventory.maxpurse")
    private val orderPattern by patternGroup.pattern(
        "order",
        ".*§6(?<coins>[\\d.,]+) coins §7each.*"
    )
    private val instantPattern by patternGroup.pattern(
        "instant",
        ".*Price per unit: §6(?<coins>[\\d.,]+) coins.*"
    )
    private val createOrderPattern by patternGroup.pattern(
        "createorder",
        "§aCreate Buy Order"
    )
    private val createInstantPattern by patternGroup.pattern(
        "createinstant",
        "§aBuy Instantly"
    )
    
    private var buyOrderPrice: Double? = null
    private var instantBuyPrice: Double? = null
    private val config get() = SkyHanniMod.feature.inventory

    private fun getPrices() {
        for (item in Minecraft.getMinecraft().thePlayer.openContainer.inventory) {
            val name = item?.displayName ?: continue
            createOrderPattern.matchMatcher(name) {
                for (info in item.getLore()) {
                    orderPattern.matchMatcher(info) {
                        // +0.1 because I expect people to use the gold nugget option
                        buyOrderPrice = group("coins").replace(",", "").toDouble() + 0.1
                        // If we get to this point, we have the instant price because instant is earlier in the list of items
                        // So we can return
                        return
                    }
                }
            }
            createInstantPattern.matchMatcher(name) {
                for (info in item.getLore()) {
                    instantPattern.matchMatcher(info) {
                        instantBuyPrice = group("coins").replace(",", "").toDouble()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
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

        val currentPurse = PurseAPI.getPurse()
        val buyOrders = buyOrderPrice?.let {
            (currentPurse / it).toInt()
        } ?: 0
        val buyInstant = instantBuyPrice?.let {
            (currentPurse / it).toInt()
        } ?: 0

        config.purseItemsPos.renderStrings(
            listOf(
                "§dYou can buy order ${buyOrders.addSeparators()}x of this item with your purse (at top order +0.1)",
                "§dYou can instantly buy ${buyInstant.addSeparators()}x of this item with your purse"
            ), posLabel = "Max Items With Purse"
        )
    }

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.maxPurseItems
    }
}
