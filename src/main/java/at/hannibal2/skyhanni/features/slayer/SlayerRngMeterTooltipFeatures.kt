package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchAllComponents
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrInsert
import at.hannibal2.skyhanni.utils.compat.getTooltip
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

@SkyHanniModule
object SlayerRngMeterTooltipFeatures {

    private val config get() = SkyHanniMod.feature.slayer

    private val patternGroup = RepoPattern.group("slayer")

    /**
     * REGEX-TEST: Revenant Horror RNG Meter
     */
    private val rngMeterSlayerTypePattern by patternGroup.pattern(
        "rngmeter.type",
        "(?<type>.+) RNG Meter",
    )

    /**
     * REGEX-TEST: Odds: RNGesus Incarnate (0.0136% 0.0174%)
     * REGEX-TEST: Odds: Occasional (20%)
     */
    private val tooltipOddsSelectedPattern by patternGroup.pattern(
        "rngmeter.tooltip.odds.selected",
        "Odds: [^(]+\\((?<old>\\d{1,2}\\.?\\d{1,4}?)% (?<new>\\d{1,2}\\.?\\d{1,4}?)%\\)",
    )

    /**
     * REGEX-TEST: Odds: Occasional (13.6082%)
     */
    private val tooltipOddsNormalPattern by patternGroup.pattern(
        "rngmeter.tooltip.odds.normal",
        "Odds: [^(]+\\((?<odds>\\d{1,2}\\.?\\d{1,4}?)%\\)",
    )

    /**
     * REGEX-TEST: SELECTED
     */
    private val tooltipIsSelectedPattern by patternGroup.pattern(
        "rngmeter.tooltip.selected",
        "SELECTED",
    )

    /**
     * REGEX-TEST: ✔ LVL 7
     */
    private val bonusRewardsLevelPattern by patternGroup.pattern(
        "bonus.tooltip.level",
        "✔ LVL (?<level>\\d)",
    )

    private val spawnCosts: Map<String, Int> = mapOf(
        "Revenant Horror" to 100_000,
        "Tarantula Broodfather" to 100_000,
        "Sven Packmaster" to 50_000,
        "Voidgloom Seraph" to 50_000,
        "Riftstalker Bloodfiend" to 10_000,
        "Inferno Demonlord" to 150_000,
    )

    @HandleEvent
    fun onRenderTooltip(event: ToolTipTextEvent) {
        if (!config.rngMeterFractions && !config.rngMeterCoinsPerBoss) return

        val slayerType = rngMeterSlayerTypePattern.matchMatcher(
            InventoryUtils.openInventoryName(),
        ) { group("type") } ?: return

        val spawnCost = spawnCosts[slayerType] ?: return
        val isSelected = tooltipIsSelectedPattern.anyMatches(event.toolTip.map { it.string.removeColor() })
        val itemPrice = event.itemStack.getInternalName().getPrice()

        for ((i, line) in event.toolTip.withIndex()) {
            if (!isSelected) {
                tooltipOddsNormalPattern.matchMatcher(line.string.removeColor()) {
                    val odds = group("odds")
                    if (config.rngMeterFractions) event.toolTip.replaceOddsWithFractions(
                        index = i,
                        firstOdds = odds,
                        secondOdds = null
                    )
                    if (config.rngMeterCoinsPerBoss) event.toolTip.addCoinsPerBoss(
                        index = i,
                        firstOdds = odds,
                        secondOdds = null,
                        itemPrice,
                        spawnCost,
                    )
                    return
                }
            } else {
                tooltipOddsSelectedPattern.matchMatcher(line.string.removeColor()) {
                    val old = group("old")
                    val new = group("new")

                    if (config.rngMeterFractions) event.toolTip.replaceOddsWithFractions(
                        index = i,
                        firstOdds = old,
                        secondOdds = new,
                    )
                    if (config.rngMeterCoinsPerBoss) event.toolTip.addCoinsPerBoss(
                        index = i,
                        firstOdds = old,
                        secondOdds = new,
                        itemPrice,
                        spawnCost,
                    )
                    return
                }
            }
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Slayer") return

        val items = event.inventoryItems

        for (item in items.values) {
            if (item.cleanName() != "Slayer Bonus Rewards") continue
            val toolTip = item.getTooltip(false)

            bonusRewardsLevelPattern.matchAll(toolTip.map { it.string.removeColor() }) {
                ProfileStorageData.profileSpecific?.slayerBonusRewardsLevel = group("level").toInt()
            }
        }
    }

    private fun MutableList<Component>.replaceOddsWithFractions(
        index: Int,
        firstOdds: String,
        secondOdds: String?
    ) {
        var line = this[index]
        val firstFraction = firstOdds.toFraction()
        val secondFraction = secondOdds?.toFraction()

        line = line.replace("$firstOdds%", "1/$firstFraction") ?: line
        secondOdds?.let { line = line.replace("$it%", "1/$secondFraction") ?: line }

        this[index] = line
    }

    private fun MutableList<Component>.addCoinsPerBoss(
        index: Int,
        firstOdds: String,
        secondOdds: String?,
        itemPrice: Double,
        spawnCost: Int,
    ) {
        val firstFraction = firstOdds.toFraction()
        val secondFraction = secondOdds?.toFraction()
        val bossesNeeded = secondFraction ?: firstFraction

        val hasPriceReduction = ProfileStorageData.profileSpecific?.slayerBonusRewardsLevel == 7
        val totalSpawnCost = bossesNeeded * (spawnCost * if (hasPriceReduction) 0.96 else 1.0)

        val profit = ((itemPrice - totalSpawnCost) / bossesNeeded).formatCoin()
        addOrInsert(index + 1, Component.empty().append("§7Coins/Boss: §6$profit"))
    }

    private fun String.toFraction(): Int = (100 / toDouble()).roundToInt()
}
