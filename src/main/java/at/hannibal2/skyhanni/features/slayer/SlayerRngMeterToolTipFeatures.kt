package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrInsert
import at.hannibal2.skyhanni.utils.compat.getTooltip
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

@SkyHanniModule
object SlayerRngMeterToolTipFeatures {

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
     * REGEX-TEST: Odds: Occasional (13.6082%)
     */
    private val toolTipOddsPattern by patternGroup.pattern(
        "rngmeter.tooltip.odds.normal",
        "Odds: [^(]+\\((?<primary>\\d{1,2}\\.?\\d{0,4})%(?: (?<secondary>\\d{1,2}\\.?\\d{0,4})%)?\\)",
    )

    /**
     * Tier V amount: 1 to 2
     * Tier IV amount: 32 to 48
     */
    private val toolTipAmountPattern by patternGroup.pattern(
        "rngmeter.tooltip.amount",
        "Tier .{1,2} amount: (?<min>\\d{1,3})(?: to (?<max>\\d{1,3}))?",
    )

    /**
     * REGEX-TEST: ✔ LVL 7
     */
    private val bonusRewardsLevelPattern by patternGroup.pattern(
        "bonus.tooltip.level",
        "✔ LVL (?<level>\\d)",
    )

    private var data: SlayerData? = null

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        val convertToFractions = config.rngMeterFractions
        val coinsPerBoss = config.rngMeterCoinsPerBoss
        if (!convertToFractions && !coinsPerBoss) return

        val slayerType = rngMeterSlayerTypePattern.matchMatcher(
            InventoryUtils.openInventoryName(),
        ) { group("type") } ?: return

        val spawnCost = data?.spawnCosts[SlayerType.getByName(slayerType)]?.values?.max() ?: return

        var minProfit = 1.0
        var maxProfit: Double? = null

        toolTipAmountPattern.matchAll(event.toolTip.map { it.string }) {
            val min = group("min").toInt()
            minProfit = SlayerApi.getItemNameAndPrice(event.itemStack.getInternalName(), min).second
            maxProfit = groupOrNull("max")?.toIntOrNull()?.let {
                SlayerApi.getItemNameAndPrice(event.itemStack.getInternalName(), it).second
            }
        }

        for ((index, line) in event.toolTip.withIndex()) {
            toolTipOddsPattern.matchMatcher(line) {
                val primary = group("primary")
                val secondary = groupOrNull("secondary")

                if (convertToFractions) {
                    event.toolTip.replaceOddsWithFractions(index, primary, secondary)
                }
                if (coinsPerBoss) {
                    event.toolTip.addCoinsPerBoss(index, primary, secondary, spawnCost, minProfit, maxProfit)
                }
                return
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

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<SlayerData>("Slayer")
    }

    private fun MutableList<Component>.replaceOddsWithFractions(
        index: Int,
        primary: String,
        secondary: String?,
    ) {
        var line = this[index]
        val firstFraction = primary.toFraction()
        val secondFraction = secondary?.toFraction()

        line = line.replace("$primary%", "1/$firstFraction") ?: line
        secondary?.let { line = line.replace("$it%", "1/$secondFraction") ?: line }

        this[index] = line
    }

    private fun MutableList<Component>.addCoinsPerBoss(
        index: Int,
        primary: String,
        secondary: String?,
        spawnCost: Int,
        minProfit: Double,
        maxProfit: Double?,
    ) {
        val bossesNeeded = (secondary ?: primary).toFraction()
        val hasPriceReduction = ProfileStorageData.profileSpecific?.slayerBonusRewardsLevel == 7
        val costPerBoss = spawnCost * if (hasPriceReduction) 0.96 else 1.0

        val line = buildString {
            append("§7Coins/Boss: ")

            maxProfit?.let {
                val maxProfitString = ((it / bossesNeeded) - costPerBoss).formatCoin()
                append("$maxProfitString §7to ")
            }

            val minProfitString = ((minProfit / bossesNeeded) - costPerBoss).formatCoin()
            append(minProfitString)
        }.let { Component.literal(it) }

        addOrInsert(index + 1, line)
    }

    private fun String.toFraction(): Int = (100 / toDouble()).roundToInt()
}
