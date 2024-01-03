package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.misc.discordrpc.purseRegex
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor

class MaxPurseItems {

    private val orderPattern by RepoPattern.pattern("inventory.maxpurse.order", ".*§6(?<coins>[\\d.,]+) coins §7each.*")
    private val instantPattern by RepoPattern.pattern(
        "inventory.maxpurse.instant",
        ".*Price per unit: §6(?<coins>[\\d.,]+) coins.*"
    )
    private val createOrderPattern by RepoPattern.pattern("inventory.maxpurse.createorder", "§aCreate Buy Order")
    private val createInstantPattern by RepoPattern.pattern("inventory.maxpurse.createinstant", "§aBuy Instantly")
    private var buyOrderPrice = 0f
    private var instantBuyPrice = 0f

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!BazaarApi.inBazaarInventory) return
        // I would use BazaarAPI for price info, but as soon as NEU's data goes out of date, it will be wrong
        if (BazaarApi.currentlyOpenedProduct == null) {
            buyOrderPrice = 0f
            instantBuyPrice = 0f
            return
        }

        items@ for (item in Minecraft.getMinecraft().thePlayer.openContainer.inventory) {
            createOrderPattern.matchMatcher(item?.displayName ?: continue) {
                if (matches()) {
                    lore@ for (info in item.getLore()) {
                        orderPattern.matchMatcher(info) {
                            if (!matches()) continue@lore
                            buyOrderPrice = group("coins").replace(",", "").toFloat()
                            // If we get to this point, we have the instant price because instant earlier in the list of items
                            // So we can return
                            break@items
                        }
                    }
                }
            }
            createInstantPattern.matchMatcher(item.displayName ?: continue) {
                if (matches()) {
                    lore@ for (info in item.getLore()) {
                        instantPattern.matchMatcher(info) {
                            if (!matches()) continue@lore
                            instantBuyPrice = group("coins").replace(",", "").toFloat()
                        }
                    }
                }
            }
        }
        val currentPurse =
            ScoreboardData.sidebarLinesFormatted.firstOrNull { purseRegex.matches(it.removeColor()) }?.let {
                purseRegex.find(it.removeColor())?.groupValues?.get(1)
            }?.replace(",", "")?.toFloat() ?: return

        var buyOrders = floor(currentPurse / buyOrderPrice).toInt()
        if (buyOrders < 0) buyOrders = 0

        var buyInstant = floor(currentPurse / instantBuyPrice).toInt()
        if (buyInstant < 0) buyInstant = 0

        SkyHanniMod.feature.inventory.purseItemsPos.renderStrings(
            listOf(
                "Buy order: ${buyOrders.addSeparators()}",
                "Buy instantly: ${buyInstant.addSeparators()}"
            ), posLabel = "Max Items With Purse"
        )
    }

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.maxPurseItems
    }
}
