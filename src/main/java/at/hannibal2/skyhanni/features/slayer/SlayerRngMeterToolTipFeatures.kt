package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrInsert
import at.hannibal2.skyhanni.utils.compat.getTooltip
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.math.ceil
import kotlin.math.roundToInt

@SkyHanniModule
object SlayerRngMeterToolTipFeatures {

    private val config get() = SkyHanniMod.feature.slayer

    private val patternGroup = RepoPattern.group("slayer")

    /**
     * REGEX-TEST: Odds: RNGesus Incarnate (0.0136% 0.0174%)
     * REGEX-TEST: Odds: Occasional (13.6082%)
     */
    private val toolTipOddsPattern by patternGroup.pattern(
        "rngmeter.tooltip.odds",
        "Odds: [^(]+\\((?<primary>\\d{1,2}\\.?\\d{0,4})%(?: (?<secondary>\\d{1,2}\\.?\\d{0,4})%)?\\)",
    )

    /**
     * REGEX-TEST: Tier V amount: 1 to 2
     * REGEX-TEST: Tier IV amount: 32 to 48
     */
    private val toolTipAmountPattern by patternGroup.pattern(
        "rngmeter.tooltip.amount",
        "Tier (?<tier>.{1,2}) amount: (?<min>\\d{1,3})(?: to (?<max>\\d{1,3}))?",
    )

    /**
     * REGEX-TEST: Slayer Bonus Rewards
     */
    private val bonusRewardsItemNamePattern by patternGroup.pattern(
        "bonus.item.name",
        "Slayer Bonus Rewards"
    )

    /**
     * REGEX-TEST: ✔ LVL 7
     */
    private val bonusRewardsLevelPattern by patternGroup.pattern(
        "bonus.tooltip.level",
        "✔ LVL (?<level>\\d)",
    )

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        val convertToFractions = config.rngMeterFractions
        val coinsPerBoss = config.rngMeterCoinsPerBoss
        if (!convertToFractions && !coinsPerBoss) return

        val slayerName = SlayerApi.rngMeterSlayerTypePattern.matchMatcher(
            InventoryUtils.openInventoryName(),
        ) { group("type") } ?: return
        val slayerType = SlayerType.getByName(slayerName)

        val internalName = event.itemStack.getInternalNameOrNull() ?: return

        var minItemPrice = 0.0
        var maxItemPrice: Double? = null
        var spawnCost = 0.0
        var scoreGainedPer = 0.0
        var scoreNeeded = 0L

        if (coinsPerBoss) {
            val slayerCosts = SlayerApi.slayerJsonData?.spawnCosts?.get(slayerType)
            val maxTier = slayerCosts?.keys?.maxOrNull()
            val tierToCalculateFor = SlayerApi.tier.takeIf { it != 0 } ?: maxTier ?: return
            spawnCost = slayerType?.calculateSpawnCost(tierToCalculateFor) ?: return

            // This goes through the list of drops per tier in the tooltip and uses the tier that we're currently calculating for
            // if it is found or the highest amount dropped overall.
            loop@ for (line in event.toolTip.map { it.string }) {
                toolTipAmountPattern.matchMatcher(line) {
                    minItemPrice = SlayerApi.getItemNameAndPrice(internalName, group("min").formatInt()).second
                    maxItemPrice = groupOrNull("max")?.formatIntOrNull()?.let {
                        SlayerApi.getItemNameAndPrice(internalName, it).second
                    }
                    if (group("tier").romanToDecimal() == tierToCalculateFor) break@loop
                }
            }

            val xpBuff = Perk.SLAYER_XP_BUFF.isActive
            val baseGained = SlayerApi.slayerJsonData?.xpGains?.get(slayerType)?.get(tierToCalculateFor) ?: return
            scoreGainedPer = baseGained * (if (xpBuff) 1.25 else 1.0)
            scoreNeeded = SlayerRngMeterDisplay.rngScore[slayerName]?.get(internalName) ?: return
        }

        for ((index, line) in event.toolTip.withIndex()) {
            toolTipOddsPattern.matchMatcher(line) {
                if (convertToFractions) {
                    event.toolTip.replaceOddsWithFractions(index, group("primary"), groupOrNull("secondary"))
                }

                if (coinsPerBoss) {
                    event.toolTip.addProfitPerBoss(index, scoreNeeded, scoreGainedPer, spawnCost, minItemPrice, maxItemPrice)
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
            if (!bonusRewardsItemNamePattern.matches(item.cleanName())) continue
            val toolTip = item.getTooltip(false)

            bonusRewardsLevelPattern.matchAll(toolTip.map { it.string.removeColor() }) {
                ProfileStorageData.profileSpecific?.slayerBonusRewardsLevel = group("level").formatInt()
            }
        }
    }

    @HandleEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (event.reason != PurseChangeCause.LOSE_SLAYER_QUEST_STARTED) return

        val expectedCoins = SlayerApi.slayerJsonData?.spawnCosts[SlayerApi.activeType]?.get(SlayerApi.tier) ?: return
        val changeNegation = (event.coins * -1).roundToInt()

        val hasSlayerBonusRewards = changeNegation == (expectedCoins * SlayerApi.SLAYER_COST_REDUCTION).roundToInt()
        val hasBartender = changeNegation == (expectedCoins * SlayerApi.BREWERY_CONTRIBUTION_REDUCTION).roundToInt()

        if (hasSlayerBonusRewards) ProfileStorageData.profileSpecific?.slayerBonusRewardsLevel = 7
        ProfileStorageData.profileSpecific?.slayerBreweryContributionReduction = hasBartender
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

    private fun MutableList<Component>.addProfitPerBoss(
        index: Int,
        scoreNeeded: Long,
        scoreGainedPer: Double,
        spawnCost: Double,
        minItemPrice: Double,
        maxItemPrice: Double?,
    ) {
        val bossesNeeded = ceil(scoreNeeded / scoreGainedPer).toInt()

        val line = buildString {
            append("§7Coins/Boss: ")

            append(calculateProfitPerBoss(bossesNeeded, spawnCost, minItemPrice))

            maxItemPrice?.let { append(" §7to ${calculateProfitPerBoss(bossesNeeded, spawnCost, it)}") }
        }

        addOrInsert(index + 1, Component.literal(line))
    }

    fun calculateProfitPerBoss(bossesNeeded: Int, cost: Double, itemPrice: Double): String =
        ((itemPrice / bossesNeeded) - cost).formatCoin()

    private fun String.toFraction(): Int = (100 / toDouble()).roundToInt()
}
