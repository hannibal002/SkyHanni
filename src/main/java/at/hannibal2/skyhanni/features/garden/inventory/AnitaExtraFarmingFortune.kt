package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.jsonobjects.repo.AnitaUpgradeCostsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.AnitaUpgradePrice
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object AnitaExtraFarmingFortune {
    private val config get() = GardenApi.config.anitaShop

    private val patternGroup = RepoPattern.group("garden.inventory.anita.extrafortune")

    private val anitaInventoryDetector = InventoryDetector { anitaInventoryPattern }

    /**
     * REGEX-TEST: Anita
     */
    private val anitaInventoryPattern by patternGroup.pattern(
        "inventory",
        "Anita",
    )

    /**
     * REGEX-TEST: Extra Farming Fortune
     */
    private val extraFarmingFortunePattern by patternGroup.pattern(
        "extrafarmingfortune",
        "Extra Farming Fortune",
    )

    /**
     * REGEX-TEST: Jacob's Ticket x450
     */
    private val realAmountPattern by patternGroup.pattern(
        "realamount.new",
        "Jacob's Ticket x(?<realAmount>.*)",
    )

    /**
     * REGEX-TEST: You have: +4 Farming Fortune
     */
    private val farmingFortunePattern by patternGroup.pattern(
        "farmingfortune",
        "You have: \\+(?<farmingFortune>\\d+)${SkyblockStat.FARMING_FORTUNE.hypixelIcon} Farming Fortune",
    )

    /**
     * REGEX-TEST: Click to trade!
     */
    private val clickToTradePattern by patternGroup.pattern(
        "clicktotrade",
        "Click to trade!",
    )

    private var levelPrice = mapOf<Int, AnitaUpgradePrice>()

    @HandleEvent
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!config.extraFarmingFortune) return
        if (!anitaInventoryDetector.isInside()) return
        if (!extraFarmingFortunePattern.matches(event.itemStack.cleanName)) return

        val (farmingFortune, contributionFactor) =
            parseExtraFarmingFortuneLore(event.itemStack.getCleanLore()) ?: return

        val anitaUpgrade = farmingFortune / 4
        if (anitaUpgrade > 0) {
            GardenApi.storage?.fortune?.anitaUpgrade = anitaUpgrade
        }

        var goldMedals = 0
        var jacobTickets = 0
        for ((level, price) in levelPrice) {
            if (level > anitaUpgrade) {
                goldMedals += price.goldMedals
                jacobTickets += price.jacobTickets
            }
        }
        jacobTickets = (contributionFactor * jacobTickets).toInt()

        val index = event.toolTip.indexOfFirst { clickToTradePattern.matches(it) }.let { it - 1 }
        if (index < 0) return

        val price = jacobTickets * "JACOBS_TICKET".toInternalName().getPrice()
        event.toolTip.add(index, "  §7Price: §6${price.shortFormat()} coins")
        event.toolTip.add(index, "§aJacob Tickets §8x${jacobTickets.addSeparators()}")
        event.toolTip.add(index, "§6Gold medals: §8x$goldMedals")
        event.toolTip.add(index, "§7Cost to max out")
        event.toolTip.add(index, "")

        // Others mods may add their own lines to the tooltip, so we need to find the line
        val farmingFortuneLine = event.toolTip.indexOfFirst {
            farmingFortunePattern.matches(it.string)
        }

        if (farmingFortuneLine != -1) {
            event.toolTip.add(
                farmingFortuneLine + 1,
                "§7Current Tier: §e$anitaUpgrade/${levelPrice.size}",
            )
        }
    }

    private fun parseExtraFarmingFortuneLore(
        lore: List<String>,
    ): ExtraFarmingFortuneLore? {
        val farmingFortune = farmingFortunePattern.firstMatcher(lore) {
            group("farmingFortune").formatInt()
        } ?: 0

        val realJacobTicket = realAmountPattern.firstMatcher(lore) {
            group("realAmount").formatDouble()
        } ?: 0.0

        val nextUpgrade = farmingFortune / 4 + 1
        val baseAmount = levelPrice[nextUpgrade]?.jacobTickets ?: return null

        val contributionFactor = if (baseAmount > 0) {
            realJacobTicket / baseAmount
        } else {
            0.0
        }

        return ExtraFarmingFortuneLore(
            farmingFortune = farmingFortune,
            contributionFactor = contributionFactor,
        )
    }

    private data class ExtraFarmingFortuneLore(
        val farmingFortune: Int,
        val contributionFactor: Double,
    )

    @HandleEvent
    private suspend fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<AnitaUpgradeCostsJson>("AnitaUpgradeCosts")
        levelPrice = data.levelPrice
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.extraFarmingFortune", "garden.anitaShop.extraFarmingFortune")
    }
}
