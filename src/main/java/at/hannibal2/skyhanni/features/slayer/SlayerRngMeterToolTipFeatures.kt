package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.TestCopyRngMeterValues
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstComponentMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcherWithIndex
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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
        val slayerType = SlayerType.getByName(slayerName) ?: return

        val goalNeeded = TestCopyRngMeterValues.rngScorePattern.firstComponentMatcher(event.toolTip) {
            group("xp").formatLongOrNull()
        } ?: return

        val internalName = event.itemStack.getInternalNameOrNull()
        val oddsInfo = getOddsInformation(event.toolTipStrings) ?: return

        if (convertToFractions) event.toolTip.replaceOddsWithFractions(oddsInfo)

        if (coinsPerBoss && internalName != null) {
            val maxSlayerTier = SlayerApi.jsonData?.spawnCosts?.get(slayerType)?.keys?.maxOrNull()
            val profitLine = coinsPerBossLine(internalName, goalNeeded, slayerType = slayerType, slayerTier = maxSlayerTier) ?: return
            event.toolTip.addOrInsert(oddsInfo.toolTipIndex + 1, Component.literal(profitLine))
        }
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

    @HandleEvent
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

    fun calculateProfitPerBoss(bossesNeeded: Int, cost: Double, itemPrice: Double): String =
        ((itemPrice / bossesNeeded) - cost).formatCoin()

    fun coinsPerBossLine(itemGoal: String, goalNeeded: Long, slayerType: SlayerType? = null): String? {
        val internalName = NeuInternalName.fromItemNameOrNull(itemGoal.removeColor()) ?: return null
        return coinsPerBossLine(internalName, goalNeeded, slayerType)
    }

    fun coinsPerBossLine(
        internalName: NeuInternalName,
        goalNeeded: Long,
        slayerType: SlayerType? = null,
        slayerTier: Int? = null,
    ): String? {
        val activeType = slayerType ?: SlayerApi.activeType ?: return null
        val activeTier = SlayerApi.tier.takeIf { it != 0 } ?: slayerTier ?: return null

        val (minDrop, maxDrop) = SlayerApi.getItemDropAmountForTier(internalName, activeTier)
        val itemPriceMin = SlayerApi.getItemNameAndPrice(internalName, minDrop).second
        val itemPriceMax = maxDrop?.let { SlayerApi.getItemNameAndPrice(internalName, it).second }

        val gainPerBoss = activeType.calculateXPGain(activeTier) ?: return null

        val bossesNeeded = ceil(goalNeeded.toDouble() / gainPerBoss).toInt().takeIf { it > 0 } ?: return null
        val spawnCost = activeType.calculateSpawnCost(activeTier) ?: return null

        return formatProfitPerBossLine(
            bossesNeeded,
            spawnCost,
            itemPriceMin,
            itemPriceMax,
        )
    }

    private fun formatProfitPerBossLine(
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
