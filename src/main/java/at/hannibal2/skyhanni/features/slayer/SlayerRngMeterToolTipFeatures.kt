package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcherWithIndex
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrInsert
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.math.ceil
import kotlin.math.roundToInt

@SkyHanniModule
object SlayerRngMeterToolTipFeatures {

    private val config get() = SkyHanniMod.feature.slayer

    private val patternGroup = RepoPattern.group("slayer")

    // How long can these percentages get?
    /**
     * REGEX-TEST: Odds: RNGesus Incarnate (0.0136% 0.0174%)
     * REGEX-TEST: Odds: Occasional (13.6082%)
     * REGEX-TEST: Odds: RNGesus Incarnate (0.0004% 0.0004002%)
     */
    private val oddsPattern by patternGroup.pattern(
        "rngmeter.tooltip.odds",
        "Odds: [^(]+\\((?<primary>\\d{1,2}\\.?\\d+)%(?: (?<secondary>\\d{1,2}\\.?\\d+)%)?\\)",
    )

    /**
     * REGEX-TEST: Tier V amount: 1 to 2
     * REGEX-TEST: Tier IV amount: 32 to 48
     * REGEX-TEST: Tier III amount: 1 to 2
     * REGEX-TEST: Tier II amount: 1
     * REGEX-TEST: Tier I amount: 1
     */
    private val amountPattern by patternGroup.pattern(
        "rngmeter.tooltip.amount",
        "Tier (?<tier>I{1,3}|IV|V) amount: (?<min>\\d{1,3})(?: to (?<max>\\d{1,3}))?",
    )

    /**
     * REGEX-TEST: Slayer Bonus Rewards
     */
    private val bonusRewardsItemNamePattern by patternGroup.pattern(
        "bonus.item.name",
        "Slayer Bonus Rewards",
    )

    /**
     * REGEX-TEST: ✔ LVL 7
     */
    private val bonusRewardsLevelPattern by patternGroup.pattern(
        "bonus.tooltip.level",
        "✔ LVL (?<level>\\d)",
    )

    private data class OddsInfo(
        val primary: String,
        val secondary: String?,
        val toolTipIndex: Int,
    )

    @HandleEvent
    private fun onToolTip(event: ToolTipTextEvent) {
        val convertToFractions = config.rngMeterFractions
        val coinsPerBoss = config.rngMeterCoinsPerBoss
        if (!convertToFractions && !coinsPerBoss) return

        val slayerName = SlayerApi.rngMeterSlayerTypePattern.matchMatcher(
            InventoryUtils.openInventoryName(),
        ) { group("type") } ?: return
        val slayerType = SlayerType.getByName(slayerName)

        val internalName = event.itemStack.getInternalNameOrNull()
        val oddsInfo = getOddsInformation(event.toolTipStrings) ?: return

        if (coinsPerBoss && internalName != null)
            event.toolTip.addProfitPerBoss(oddsInfo.toolTipIndex, slayerType, slayerName, internalName, event.toolTipStrings)

        if (convertToFractions) event.toolTip.replaceOddsWithFractions(oddsInfo)
    }

    private fun getOddsInformation(toolTipStrings: List<String>): OddsInfo? =
        oddsPattern.firstMatcherWithIndex(toolTipStrings) { toolTipIndex ->
            val primary = group("primary")
            val secondary = groupOrNull("secondary")
            OddsInfo(
                primary,
                secondary,
                toolTipIndex,
            )
        }

    // This goes through the list of drops per tier in the tooltip and uses the tier that we're currently calculating for
    // if it is found or the highest amount dropped overall.
    private fun loopThroughDropAmounts(
        toolTipStrings: List<String>,
        internalName: NeuInternalName,
        tierToCalculateFor: Int,
    ): Pair<Double, Double?> {
        var minItemPrice = 0.0
        var maxItemPrice: Double? = null

        loop@ for (line in toolTipStrings) {
            amountPattern.matchMatcher(line) {
                minItemPrice = SlayerApi.getItemNameAndPrice(internalName, group("min").formatInt()).second
                maxItemPrice = groupOrNull("max")?.formatIntOrNull()?.let {
                    SlayerApi.getItemNameAndPrice(internalName, it).second
                }

                if (group("tier").romanToDecimal() == tierToCalculateFor) break@loop
            }
        }

        return minItemPrice to maxItemPrice
    }

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!SlayerApi.inventoryNamePattern.matches(event.inventoryName)) return

        val items = event.inventoryItems

        for (item in items.values) {

            if (!bonusRewardsItemNamePattern.matches(item.cleanName)) continue
            val toolTip = item.getCleanLore()

            bonusRewardsLevelPattern.matchAll(toolTip) {
                SlayerApi.updateBonusRewardsLevel(group("level").formatInt())
            }
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onPurseChange(event: PurseChangeEvent) {
        if (event.reason != PurseChangeCause.LOSE_SLAYER_QUEST_STARTED) return

        val expectedCoins = SlayerApi.activeType?.calculateSpawnCost(SlayerApi.tier, includeReduction = false) ?: return
        val changeNegation = (event.coins * -1).roundToInt()

        val hasSlayerBonusRewards = changeNegation == (expectedCoins * SlayerApi.COST_REDUCTION).roundToInt()
        val hasBartender = changeNegation == (expectedCoins * SlayerApi.BREWERY_CONTRIBUTION_REDUCTION).roundToInt()

        if (hasSlayerBonusRewards) SlayerApi.updateBonusRewardsLevel(SlayerApi.COST_REDUCTION_LEVEL)
        SlayerApi.updateBreweryContribution(hasBartender)
    }

    private fun MutableList<Component>.replaceOddsWithFractions(
        oddsInfo: OddsInfo,
    ) {
        val (primary, secondary, index) = oddsInfo
        var line = this[index]
        val primaryFraction = primary.toFraction()
        val secondaryFraction = secondary?.toFraction()

        line = line.replace("$primary%", "1/$primaryFraction") ?: line
        secondary?.let { line = line.replace("$it%", "1/$secondaryFraction") ?: line }

        this[index] = line
    }

    private fun MutableList<Component>.addProfitPerBoss(
        index: Int,
        slayerType: SlayerType?,
        slayerName: String,
        internalName: NeuInternalName,
        toolTipStrings: List<String>,
    ) {
        val slayerCosts = SlayerApi.jsonData?.spawnCosts?.get(slayerType)
        val maxTier = slayerCosts?.keys?.maxOrNull()
        val tierToCalculateFor = SlayerApi.tier.takeIf { it != 0 } ?: maxTier ?: return
        val spawnCost = slayerType?.calculateSpawnCost(tierToCalculateFor) ?: return

        val (minItemPrice, maxItemPrice) = loopThroughDropAmounts(toolTipStrings, internalName, tierToCalculateFor)

        val scoreGainedPer = slayerType.calculateXPGain(tierToCalculateFor) ?: return
        val scoreNeeded = SlayerRngMeterDisplay.rngScore[slayerName]?.get(internalName) ?: return

        val bossesNeeded = ceil(scoreNeeded / scoreGainedPer).toInt()

        val line = formatProfitPerBossLine(bossesNeeded, spawnCost, minItemPrice, maxItemPrice)

        addOrInsert(index + 1, Component.literal(line))
    }

    fun calculateProfitPerBoss(bossesNeeded: Int, cost: Double, itemPrice: Double): String =
        ((itemPrice / bossesNeeded) - cost).formatCoin()

    fun formatProfitPerBossLine(
        bossesNeeded: Int,
        spawnCost: Double,
        minPrice: Double,
        maxPrice: Double? = null,
    ): String = buildString {
        append("§7Coins/Boss: ")
        append(calculateProfitPerBoss(bossesNeeded, spawnCost, minPrice))
        maxPrice?.let {
            if (it != minPrice) {
                append(" §7to ${calculateProfitPerBoss(bossesNeeded, spawnCost, it)}")
            }
        }
    }

    private fun String.toFraction(): Int = (100 / toDouble()).roundToInt()
}
