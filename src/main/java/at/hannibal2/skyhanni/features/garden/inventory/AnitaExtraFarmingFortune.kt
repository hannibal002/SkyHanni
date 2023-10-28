package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.indexOfFirst
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.jsonobjects.AnitaUpgradeCostsJson
import at.hannibal2.skyhanni.utils.jsonobjects.AnitaUpgradeCostsJson.Price
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AnitaExtraFarmingFortune {
    private val config get() = SkyHanniMod.feature.garden.anitaShop
    private var levelPrice = mapOf<Int, Price>()

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!config.extraFarmingFortune) return

        if (InventoryUtils.openInventoryName() != "Anita") return

        val stack = event.itemStack ?: return

        if (!stack.displayName.contains("Extra Farming Fortune")) return

        val anitaUpgrade = GardenAPI.config?.fortune?.anitaUpgrade ?: return

        var contributionFactor = 1.0
        val baseAmount = levelPrice[anitaUpgrade + 1]?.jacob_tickets ?: return
        for (line in event.toolTip) {
            "§5§o§aJacob's Ticket §8x(?<realAmount>.*)".toPattern().matchMatcher(line) {
                val realAmount = group("realAmount").formatNumber().toDouble()
                contributionFactor = realAmount / baseAmount
            }
        }

        var goldMedals = 0
        var jacobTickets = 0
        for ((level, price) in levelPrice) {
            if (level > anitaUpgrade) {
                goldMedals += price.gold_medals
                jacobTickets += price.jacob_tickets
            }
        }
        jacobTickets = (contributionFactor * jacobTickets).toInt()

        val index = event.toolTip.indexOfFirst("§5§o§eClick to trade!")?.let { it - 1 } ?: return

        // TODO: maybe only show the price when playing classic
//        if (!LorenzUtils.noTradeMode) {
        val price = jacobTickets * "JACOBS_TICKET".asInternalName().getPrice()
        event.toolTip.add(index, "  §7Price: §6${NumberUtil.format(price)} coins")
//        }
        event.toolTip.add(index, "§aJacob Tickets §8x${jacobTickets.addSeparators()}")
        event.toolTip.add(index, "§6Gold medals: §8x$goldMedals")
        event.toolTip.add(index, "§7Cost to max out")
        event.toolTip.add(index, "")

        val upgradeIndex = event.toolTip.indexOfFirst { it.contains("You have") }
        if (upgradeIndex != -1) {
            event.toolTip.add(upgradeIndex + 1, "§7Current Tier: §e$anitaUpgrade/${levelPrice.size}")
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<AnitaUpgradeCostsJson>("AnitaUpgradeCosts")
        levelPrice = data.level_price
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.extraFarmingFortune", "garden.anitaShop.extraFarmingFortune")
    }
}
