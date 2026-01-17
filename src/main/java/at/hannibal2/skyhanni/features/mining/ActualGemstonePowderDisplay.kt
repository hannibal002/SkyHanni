package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningEventsApi
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderChestReward
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.Locale

@SkyHanniModule
object ActualGemstonePowderDisplay {

    private val config get() = SkyHanniMod.feature.chat
    private val patternGroup = RepoPattern.group("mining.powder.multiplier")

    /**
     * REGEX-TEST: §7§7Grants §d+50% §dGemstone Powder§7, and
     */
    private val drillBasePowderPattern by patternGroup.pattern(
        "drill.powder.base",
        "§7§7Grants §d\\+(?<bonus>\\d+)% §dGemstone Powder§7, and"
    )

    /**
     * REGEX-TEST: §7Earn §9+25% Powder §7from all sources.
     */
    private val drillUpgradePowderPattern by patternGroup.pattern(
        "drill.powder.upgrade",
        "§7Earn §9\\+(?<bonus>\\d+)% Powder §7from all sources."
    )

    private enum class PowderStyle(
        val indent: String,
        val labelColor: String,
        val valueColor: String
    ) {
        CATEGORY("§7└ ", "§7", "§a"),
        DETAIL("§8  └ ", "§8", "§2"),
        SUB_DETAIL("§5    └ ", "§5", "§b")
    }

    private fun Double.formatDec() = String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    private fun Double.toMultiplier(): String = "x${this.formatDec()}"
    private fun Double.toPercent(): String {
        val percent = this * 100.0
        return if (percent % 1.0 == 0.0) "+${percent.toInt()}%"
        else "+${String.format(Locale.US, "%.2f", percent).trimEnd('0').trimEnd('.')}%"
    }


    /**
     * Represents a source that contributes to the final powder multiplier.
     * @property bonusFraction The fractional bonus this specific factor contributes (e.g., 0.15 for +15%).
     * @property multiplier The actual mathematical multiplier (1.0 + bonusFraction + sum_of_children_bonuses).
     */
    private sealed interface MultiplierFactor {
        val name: String
        val bonusFraction: Double
        val factors: List<MultiplierFactor>
        val multiplier: Double
        fun getTooltipLines(level: Int = 1): List<String>
    }

    private fun formatLine(factor: MultiplierFactor, level: Int): String {
        val style = when (level) {
            1 -> PowderStyle.CATEGORY
            2 -> PowderStyle.DETAIL
            else -> PowderStyle.SUB_DETAIL
        }

        val displayValue = if (level == 1) {
            factor.multiplier.toMultiplier()
        } else {
            factor.bonusFraction.toPercent()
        }

        return "${style.indent}${style.labelColor}${factor.name}: ${style.valueColor}$displayValue"
    }

    /**
     * Represents an additive group where children's bonuses are summed.
     * Formula: 1 + bonusFraction + Sum(children.bonusFraction)
     * Note: Since drill components don't have their own children, we treat them as level-3 SimpleFactors.
     */
    private class AdditiveFactor(
        override val name: String,
        override val factors: List<MultiplierFactor> = emptyList()
    ) : MultiplierFactor {

        override val bonusFraction: Double
            get() = factors.sumOf { it.bonusFraction }

        override val multiplier: Double
            get() = 1.0 + bonusFraction

        override fun getTooltipLines(level: Int): List<String> = buildList {
            if (multiplier <= 1.0) return@buildList

            add(formatLine(this@AdditiveFactor, level))

            factors.forEach { child ->
                addAll(child.getTooltipLines(level + 1))
            }
        }
    }

    /**
     * Represents a simple factor that has no children or represents a leaf component.
     * Multipliers like events (x2) or leaf components (Drill Base +20%).
     */
    private class SimpleFactor(
        override val name: String,
        override val bonusFraction: Double,
        override val factors: List<MultiplierFactor> = emptyList()
    ) : MultiplierFactor {
        override val multiplier: Double = 1.0 + bonusFraction

        override fun getTooltipLines(level: Int): List<String> = buildList {
            if (bonusFraction <= 0) return@buildList

            add(formatLine(this@SimpleFactor, level))
        }
    }

    private data class AttributeShardScaling(
        val shardName: String,
        val divisor: Int,
    )

    private val ATOMIZED_CRYSTALS_SHARD = AttributeShardScaling("SHARD_THYST", 1)
    private val ECHO_OF_ATOMIZED_SHARD = AttributeShardScaling("SHARD_IGUANA", 2)
    private val ECHO_OF_ECHOES_SHARD = AttributeShardScaling("SHARD_TIAMAT", 5)

    private fun getFactors(): List<MultiplierFactor> = buildList {
        val attributeFactors = mutableListOf<MultiplierFactor>()

        val acLevel = AttributeShardsData.getActiveLevel(ATOMIZED_CRYSTALS_SHARD.shardName)
        val acRate = (acLevel * ATOMIZED_CRYSTALS_SHARD.divisor) / 100.0
        if (acRate > 0) attributeFactors.add(SimpleFactor("Atomized Crystals [$acLevel]", acRate))

        val eaLevel = AttributeShardsData.getActiveLevel(ECHO_OF_ATOMIZED_SHARD.shardName)
        val eaRate = (eaLevel * ECHO_OF_ATOMIZED_SHARD.divisor) / 100.0
        val eaContrib = acRate * eaRate
        if (eaContrib > 0) attributeFactors.add(SimpleFactor("Echo of Atomized [$eaLevel]", eaContrib))

        val eeLevel = AttributeShardsData.getActiveLevel(ECHO_OF_ECHOES_SHARD.shardName)
        val eeRate = (eeLevel * ECHO_OF_ECHOES_SHARD.divisor) / 100.0
        val eeContrib = eaContrib * eeRate
        if (eeContrib > 0) attributeFactors.add(SimpleFactor("Echo of Echoes [$eeLevel]", eeContrib))

        if (attributeFactors.isNotEmpty()) {
            add(AdditiveFactor("Attribute Bonus", factors = attributeFactors))
        }

        val hotmFactors = mutableListOf<MultiplierFactor>()

        val hotmLevel = HotmData.POWDER_BUFF.activeLevel
        if (hotmLevel > 0) {
            hotmFactors.add(SimpleFactor("HOTM Powder Buff", (hotmLevel / 100.0)))
        }

        val heldItem = InventoryUtils.getItemInHand()
        if (heldItem != null && heldItem.getItemCategoryOrNull() == ItemCategory.DRILL) {
            val drillFactors = mutableListOf<MultiplierFactor>()
            val lore = heldItem.getLore()
            var baseDrill = 0.0
            var upgradeDrill = 0.0

            lore.forEach { line ->
                drillBasePowderPattern.matchMatcher(line) { baseDrill += group("bonus").toInt() }
                drillUpgradePowderPattern.matchMatcher(line) { upgradeDrill += group("bonus").toInt() }
            }

            if (baseDrill > 0) drillFactors.add(SimpleFactor("Drill Base", baseDrill / 100.0))
            if (upgradeDrill > 0) drillFactors.add(SimpleFactor("Goblin Egg", upgradeDrill / 100.0))

            if (drillFactors.isNotEmpty()) {
                hotmFactors.add(AdditiveFactor(heldItem.displayName.formattedTextCompat(), factors = drillFactors))
            }
        }

        if (hotmFactors.isNotEmpty()) {
            add(AdditiveFactor("HOTM Powder Buffs", factors = hotmFactors))
        }


        if (MiningEventsApi.isMiningEventActive(MiningEventsApi.MiningEventType.DOUBLE_POWDER)) {
            add(SimpleFactor("2x Powder Event", 1.0))
        }

        if (HotmData.SKY_MALL.enabled && HotmApi.skymall == HotmApi.SkymallPerk.EXTRA_POWDER) {
            add(SimpleFactor("Sky Mall", 0.15))
        }

        val pet = CurrentPetApi.currentPet
        if (pet != null && pet.cleanName == "Scatha" && pet.rarity == LorenzRarity.LEGENDARY) {
            val petBonus = (0.2 * pet.level) / 100.0
            add(SimpleFactor("[Lvl ${pet.level}] ${pet.coloredName}", petBonus))
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        PowderChestReward.GEMSTONE_POWDER.chatPattern.matchMatcher(event.message) {
            val amountStr = groupOrNull("amount") ?: return
            val originalAmount = amountStr.formatInt()

            val factors = getFactors()
            val totalMultiplier = factors.fold(1.0) { acc, factor -> acc * factor.multiplier }

            val actualAmount = (originalAmount * totalMultiplier).toInt()

            if (totalMultiplier > 1.0) {

                val hoverText = buildList {
                    add("§7Base Powder: §e${originalAmount.addSeparators()}")
                    add("")
                    add("§7Total Multiplier: §6${totalMultiplier.toMultiplier()}")
                    factors.forEach { factor ->
                        addAll(factor.getTooltipLines(level = 1))
                    }
                    add("")
                    add("§7Actual Powder: §d${actualAmount.addSeparators()}")
                }

                event.chatComponent = TextHelper.text(
                    text = "    §r§dGemstone Powder §r§8x${originalAmount.addSeparators()} §7(x${actualAmount.addSeparators()})"
                ) {
                    this.hover = TextHelper.multiline(hoverText)
                }
            }
        }
    }

    private fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.showEffectivePowder
}
