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
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
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
        "You have: \\+(?<farmingFortune>\\d+)${SkyblockStat.FARMING_FORTUNE.hypixelIcon} Farming Fortune"
    )

    private var levelPrice = mapOf<Int, AnitaUpgradePrice>()

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!config.extraFarmingFortune) return
        if (!anitaInventoryDetector.isInside()) return
        if (!extraFarmingFortunePattern.matches(event.itemStack.cleanName)) return
        val (farmingFortune, farmingFortuneLine, contributionFactor) =
            parseExtraFarmingFortuneLore(event.itemStack.getCleanLore()) ?: return

        val anitaUpgrade = farmingFortune / 4
        GardenApi.storage?.fortune?.anitaUpgrade = anitaUpgrade

        var goldMedals = 0
        var jacobTickets = 0
        for ((level, price) in levelPrice) {
            if (level > anitaUpgrade) {
                goldMedals += price.goldMedals
                jacobTickets += price.jacobTickets
            }
        }
        jacobTickets = (contributionFactor * jacobTickets).toInt()

        val index = event.toolTip.lastIndex - 1
        if (index < 0) return

        val price = jacobTickets * "JACOBS_TICKET".toInternalName().getPrice()
        event.toolTip.add(index, "  §7Price: §6${price.shortFormat()} coins")

        event.toolTip.add(index, "§aJacob Tickets §8x${jacobTickets.addSeparators()}")
        event.toolTip.add(index, "§6Gold medals: §8x$goldMedals")
        event.toolTip.add(index, "§7Cost to max out")
        event.toolTip.add(index, "")

        if (farmingFortuneLine != null && farmingFortuneLine + 2 < event.toolTip.size) {
            event.toolTip.add(
                farmingFortuneLine + 2,
                "§7Current Tier: §e$anitaUpgrade/${levelPrice.size}",
            )
        }
    }

    private fun parseExtraFarmingFortuneLore(
        lore: List<String>,
    ): ExtraFarmingFortuneLore? {
        var farmingFortune = 0
        var farmingFortuneLine: Int? = null
        var contributionFactor = 1.0

        for ((index, line) in lore.withIndex()) {
            farmingFortunePattern.matchMatcher(line) {
                farmingFortune = group("farmingFortune").toInt()
                farmingFortuneLine = index
            }

            realAmountPattern.matchMatcher(line) {
                contributionFactor = group("realAmount").formatDouble()
            }
        }

        val fortune = farmingFortune ?: return null
        val nextUpgrade = fortune / 4 + 1
        val baseAmount = levelPrice[nextUpgrade]?.jacobTickets ?: return null

        if (baseAmount > 0) {
            contributionFactor /= baseAmount
        } else {
            contributionFactor = 1.0
        }

        return ExtraFarmingFortuneLore(
            farmingFortune = fortune,
            farmingFortuneLine = farmingFortuneLine,
            contributionFactor = contributionFactor,
        )
    }

    private data class ExtraFarmingFortuneLore(
        val farmingFortune: Int,
        val farmingFortuneLine: Int?,
        val contributionFactor: Double,
    )

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<AnitaUpgradeCostsJson>("AnitaUpgradeCosts")
        levelPrice = data.levelPrice
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.extraFarmingFortune", "garden.anitaShop.extraFarmingFortune")
    }
}
