package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderChestReward
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningEventsApi
import at.hannibal2.skyhanni.events.mining.MiningEventEvent
import at.hannibal2.skyhanni.utils.ChatUtils
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
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.Locale

@SkyHanniModule
object ActualGemstonePowderDisplay {

    private data class AttributeShardScaling(
        val shardName: String,
        val divisor: Int, // ie 5 for 5% per level
    )

    private val ATOMIZED_CRYSTALS_SHARD = AttributeShardScaling("SHARD_THYST", 1)
    private val ECHO_OF_ATOMIZED_SHARD = AttributeShardScaling("SHARD_IGUANA", 2)
    private val ECHO_OF_ECHOES_SHARD = AttributeShardScaling("SHARD_TIAMAT", 5)

    private data class AttributeStats(
        val level: Int,
        val baseRate: Double,
        val actualBonus: Double
    )

    private data class PetStats(
        val petName: String?,
        val petLevel: Int,
        val petMultiplier: Double
    )

    private data class DrillStats(
        val name: String,
        val basePercent: Int,
        val upgradePercent: Int,
        val totalBonusFraction: Double
    )

    private data class MultiplierBreakdown(
        val atomized: AttributeStats,
        val echoAtomized: AttributeStats,
        val echoEchoes: AttributeStats,
        val drill: DrillStats,
        val pet: PetStats,
        val hotmBuffPercent: Int,
        val totalAttributeBonusFraction: Double,
        val totalMultiplier: Double
    )


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


    private fun getAttributeStats(scaling: AttributeShardScaling): Pair<Int, Double> {
        val level = AttributeShardsData.getActiveLevel(scaling.shardName)
        val rate = (level * scaling.divisor) / 100.0
        return level to rate
    }

    private fun getPetStats(): PetStats {
        val pet = CurrentPetApi.currentPet ?: return PetStats(null, 0, 1.0)
        var bonusPercent = 0.0

        if (pet.cleanName == "Scatha" && pet.rarity == LorenzRarity.LEGENDARY) {
            bonusPercent = 0.2 * pet.level
        }

        val multiplier = 1.0 + (bonusPercent / 100.0)

        return PetStats(
            petName = pet.cleanName,
            petLevel = pet.level,
            petMultiplier = multiplier
        )
    }

    private fun getDrillStats(): DrillStats {
        var basePercent = 0
        var upgradePercent = 0
        var drillName = "None"

        val heldItem = InventoryUtils.getItemInHand()
        if (heldItem == null || heldItem.getItemCategoryOrNull() != ItemCategory.DRILL) {
            return DrillStats(drillName, basePercent, upgradePercent, 1.0)
        }

        drillName = heldItem.displayName

        val lore = heldItem.getLore()

        lore.forEach { line ->
            drillBasePowderPattern.matchMatcher(line) {
                basePercent += group("bonus").toInt()
            }
            drillUpgradePowderPattern.matchMatcher(line) {
                upgradePercent += group("bonus").toInt()
            }
        }

        val totalBonusPercent = basePercent + upgradePercent
        val totalBonusFraction = 1.0 + (totalBonusPercent / 100.0)

        return DrillStats(
            name = drillName,
            basePercent = basePercent,
            upgradePercent = upgradePercent,
            totalBonusFraction = totalBonusFraction
        )
    }

    private fun calculateMultiplierBreakdown(): MultiplierBreakdown {
        val (acLevel, acRate) = getAttributeStats(ATOMIZED_CRYSTALS_SHARD)
        val acStats = AttributeStats(acLevel, acRate, acRate)
        val (eaLevel, eaRate) = getAttributeStats(ECHO_OF_ATOMIZED_SHARD)
        val eaContribution = acStats.actualBonus * eaRate
        val eaStats = AttributeStats(eaLevel, eaRate, eaContribution)
        val (eeLevel, eeRate) = getAttributeStats(ECHO_OF_ECHOES_SHARD)
        val eeContribution = eaStats.actualBonus * eeRate
        val eeStats = AttributeStats(eeLevel, eeRate, eeContribution)

        val totalAttributeBonus = acStats.actualBonus + eaStats.actualBonus + eeStats.actualBonus
        val totalAttributeBonusFraction = 1.0 + totalAttributeBonus

        val drillStats = getDrillStats()
        val hotmBuffPercent = HotmData.POWDER_BUFF.activeLevel

        val additivePercentSum = hotmBuffPercent + (drillStats.totalBonusFraction - 1.0) * 100.0

        val hotmDrillFactor = 1.0 + additivePercentSum / 100.0 // NEW FIELD

        var currentMultiplier = totalAttributeBonusFraction * hotmDrillFactor

        val is2xActive = MiningEventsApi.isMiningEventActive(MiningEventsApi.MiningEventType.DOUBLE_POWDER)
        if (is2xActive) currentMultiplier *= 2.0

        val isSkymallActive = HotmData.SKY_MALL.enabled && HotmApi.skymall == HotmApi.SkymallPerk.EXTRA_POWDER
        if (isSkymallActive) currentMultiplier *= 1.15

        val petStats = getPetStats()
        currentMultiplier *= petStats.petMultiplier

        return MultiplierBreakdown(
            atomized = acStats,
            echoAtomized = eaStats,
            echoEchoes = eeStats,
            totalAttributeBonusFraction = totalAttributeBonusFraction,
            hotmBuffPercent = hotmBuffPercent,
            drill = drillStats,
            pet = petStats,
            totalMultiplier = currentMultiplier
        )
    }

    @HandleEvent
    fun onMiningEventStarted(event: MiningEventEvent.Started) {
        ChatUtils.debug("[Powder Debug] Mining event Started Triggered ${event.event.type}")
    }

    @HandleEvent
    fun onMiningEventEnded(event: MiningEventEvent.Ended) {
        ChatUtils.debug("[Powder Debug] Mining event Ended Triggered ${event.event.type}")
    }

    private fun MultiplierBreakdown.toTooltipLines(baseAmount: Int, actualAmount: Int): List<String> = buildList {
        val lines = mutableListOf<String>()

        fun Double.formatPretty(): String {
            return if (this % 1.0 == 0.0) this.toInt().toString()
            else String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
        }
        fun Double.toPercentStr() = (this * 100.0).formatPretty()

        lines.add("§7Base Powder: §e${baseAmount.addSeparators()}")
        lines.add("§7Multiplier Breakdown:")
        lines.add("")

        val attributeBonusPercent = (totalAttributeBonusFraction - 1.0) * 100.0
        lines.add("§6§lAdditive Multipliers:")

        lines.add("§7 └ Attributes Bonus: §a+${attributeBonusPercent.formatPretty()}%")
        if (atomized.level > 0) lines.add("§8   └ Atomized [${atomized.level}]: §2+${atomized.actualBonus.toPercentStr()}%")
        if (echoAtomized.level > 0) lines.add("§8   └ Echo of Atomized [${echoAtomized.level}]: §2+${echoAtomized.actualBonus.toPercentStr()}%")
        if (echoEchoes.level > 0) lines.add("§8   └ Echo of Echoes [${echoEchoes.level}]: §2+${echoEchoes.actualBonus.toPercentStr()}%")

        lines.add("")

        lines.add("§6§lStacking Multipliers:")


        if (hotmBuffPercent > 0) {
            lines.add("§7 └ HOTM Powder Buff: §a+${hotmBuffPercent}%")
        }

        val drillBonusPercent = (drill.totalBonusFraction - 1.0) * 100.0
        if (drillBonusPercent > 0.0) {
            lines.add("§7 └ ${drill.name}: §a+${drillBonusPercent.formatPretty()}%")
            if (drill.basePercent > 0) lines.add("§8   └ Drill Base: §2+${drill.basePercent}%")
            if (drill.upgradePercent > 0) lines.add("§8   └ Goblin Egg: §2+${drill.upgradePercent}%")
        }

        val is2xActive = MiningEventsApi.isMiningEventActive(MiningEventsApi.MiningEventType.DOUBLE_POWDER)
        if (is2xActive) lines.add("§7 └ 2x Powder Event: §a+100%")

        val isSkymallActive = HotmData.SKY_MALL.enabled && HotmApi.skymall == HotmApi.SkymallPerk.EXTRA_POWDER
        if (isSkymallActive) lines.add("§7 └ Sky Mall: §a+15%")

        val petBonusPercent = (pet.petMultiplier - 1) * 100
        if (petBonusPercent > 0.0 && pet.petName != null) {
            lines.add("§7 └ [Lvl ${pet.petLevel}] ${LorenzRarity.LEGENDARY.color.getChatColor()}${pet.petName}: §a+${petBonusPercent.formatPretty()}%")
        }

        lines.add("")
        lines.add("§7Total Multiplier: §6${totalMultiplier.formatPretty()}x")
        lines.add("§7Actual Powder: §d${actualAmount.addSeparators()}")

        return lines
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        PowderChestReward.GEMSTONE_POWDER.chatPattern.matchMatcher(event.message) {
            val amountStr = groupOrNull("amount") ?: return
            val originalAmount = amountStr.formatInt()

            val breakdown = calculateMultiplierBreakdown()

            val actualAmount = (originalAmount * breakdown.totalMultiplier).toInt()

            ChatUtils.debug("[PowderDebug] Base: $originalAmount, Effective: $actualAmount, Multiplier: ${breakdown.totalMultiplier}")

            if (breakdown.totalMultiplier > 1.0) {
                val originalFormatted = originalAmount.addSeparators()
                val actualFormatted = actualAmount.addSeparators()

                val hoverText = breakdown.toTooltipLines(originalAmount, actualAmount)

                event.chatComponent = TextHelper.text("    §r§dGemstone Powder §r§8x$originalFormatted §7(x$actualFormatted)") {
                    this.hover = TextHelper.multiline(hoverText)
                }
            }
        }
    }


    private fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.showEffectivePowder
}
