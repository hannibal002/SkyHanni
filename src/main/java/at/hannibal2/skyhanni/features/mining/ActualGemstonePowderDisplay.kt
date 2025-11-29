package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderChestReward
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeShardsData
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import java.util.Locale

@SkyHanniModule
object ActualGemstonePowderDisplay {

    private const val ATOMIZED_CRYSTALS_SHARD = "SHARD_THYST"
    private const val ECHO_OF_ATOMIZED_SHARD = "SHARD_IGUANA"

    private data class MultiplierBreakdown(
        val atomizedCrystals: Double,
        val echoOfAtomizedRate: Double,
        val is2xEventActive: Boolean,
        val isSkymallActive: Boolean,
        val hotmBuffPercent: Int,
        val drillMultiplier: Double,
        val petMultiplier: Double,
        val petName: String?,
        val totalMultiplier: Double
    )


    private val config get() = SkyHanniMod.feature.chat

    private var is2xPowderActive = false

    private val patternGroup = RepoPattern.group("mining.powder.multiplier")

    /**
     * REGEX-TEST: §e§lPASSIVE EVENT §b§l2X POWDER §e§lRUNNING FOR §a§l0:30§r
     */
    private val powderBossBarPattern by patternGroup.pattern(
        "powder.bossbar",
        "§e§lPASSIVE EVENT §b§l2X POWDER §e§lRUNNING FOR §a§l(?<time>.*)§r",
    )

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


    private fun getAtomizedCrystalsBonus(): Double {
        val level = AttributeShardsData.getActiveLevel(ATOMIZED_CRYSTALS_SHARD)
        return level / 100.0
    }

    private fun getEchoOfAtomizedBonusRate(): Double {
        val level = AttributeShardsData.getActiveLevel(ECHO_OF_ATOMIZED_SHARD)
        val bonusPercent = level * 2.0
        return bonusPercent / 100.0
    }


    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return

        is2xPowderActive = false

        if (TabWidget.EVENT.isActive) {
            for (line in TabWidget.EVENT.lines) {
                if (line.contains("2x Powder")) {
                    is2xPowderActive = true
                    break
                }
            }
        } else {
            powderBossBarPattern.matchMatcher(BossbarData.getBossbar()) {
                is2xPowderActive = true
            }
        }
    }


    private fun calculateMultiplierBreakdown(): MultiplierBreakdown {

        var additiveBonus = 0.0

        val atomizedCrystalsMulti = getAtomizedCrystalsBonus()
        val echoOfAtomizedRate = getEchoOfAtomizedBonusRate()

        additiveBonus += atomizedCrystalsMulti

        val echoContribution = atomizedCrystalsMulti * echoOfAtomizedRate
        ChatUtils.debug("[PowderDebug] Echo Contribution: ${echoContribution * 100}%")
        additiveBonus += echoContribution

        var totalMultiplier = 1.0 + additiveBonus

        val is2xActive = is2xPowderActive
        if (is2xActive) {
            totalMultiplier *= 2.0
        }

        val isSkymallActive = HotmData.SKY_MALL.enabled && HotmApi.skymall == HotmApi.SkymallPerk.EXTRA_POWDER
        if (isSkymallActive) {
            totalMultiplier *= 1.15
        }



        val hotmBuffPercent = HotmData.POWDER_BUFF.activeLevel

        val drillFractional = getDrillMultiplierFraction()
        val drillAdditiveBonusPercent = ((drillFractional - 1) * 100.0)
        ChatUtils.debug("[PowderDebug] drillAdditiveBonusPercent: $drillAdditiveBonusPercent")

        val combinedAdditivePercent = hotmBuffPercent + drillAdditiveBonusPercent

        ChatUtils.debug("[PowderDebug] combinedAdditivePercent: $combinedAdditivePercent")

        if (combinedAdditivePercent > 0) {
            totalMultiplier *= (1.0 + combinedAdditivePercent / 100.0)
        }

        val petFractional = getPetMultiplierFraction()
        val petName = if (petFractional > 1.0) {
            CurrentPetApi.currentPet?.cleanName
        } else null

        totalMultiplier *= petFractional

        ChatUtils.debug("[PowderDebug] Total Multiplier: $totalMultiplier")

        return MultiplierBreakdown(
            atomizedCrystals = atomizedCrystalsMulti,
            echoOfAtomizedRate = echoOfAtomizedRate,
            is2xEventActive = is2xActive,
            isSkymallActive = isSkymallActive,
            hotmBuffPercent = hotmBuffPercent,
            drillMultiplier = drillFractional,
            petMultiplier = petFractional,
            petName = petName,
            totalMultiplier = totalMultiplier
        )
    }

    private fun MultiplierBreakdown.toTooltipLines(baseAmount: Int, effectiveAmount: Int): List<String> = buildList {
        val lines = mutableListOf<String>()

        fun Double.formatPretty(): String {
            return if (this % 1.0 == 0.0) {
                this.toInt().toString()
            } else {
                String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
            }
        }

        lines.add("§7Base Powder: §e${baseAmount.addSeparators()}")
        lines.add("§7Multiplier Breakdown:")
        lines.add("")

        if (atomizedCrystals > 0.0) {
            val basePercent = atomizedCrystals * 100
            val echoPercent = echoOfAtomizedRate * 100

            val totalAddedPercent = basePercent + (basePercent * echoOfAtomizedRate)

            val sb = StringBuilder()
            sb.append("§7 Attributes: §a+${totalAddedPercent.formatPretty()}%")

            if (echoOfAtomizedRate > 0.0) {
                val baseStr = basePercent.formatPretty()
                val echoStr = echoPercent.formatPretty()
                sb.append(" §7($baseStr% + $echoStr% of $baseStr%)")
            }

            val atomizedLevel = AttributeShardsData.getActiveLevel(ATOMIZED_CRYSTALS_SHARD)
            val echoLevel = AttributeShardsData.getActiveLevel(ECHO_OF_ATOMIZED_SHARD)
            sb.append(" §8[AC:$atomizedLevel EC:$echoLevel]")

            lines.add(sb.toString())
        }

        if (is2xEventActive) {
            lines.add("§7 2x Powder Event: §a+100%")
        }

        if (isSkymallActive) {
            lines.add("§7 Sky Mall: §a+15%")
        }

        if (hotmBuffPercent > 0) {
            lines.add("§7 HOTM Powder Buff: §a+$hotmBuffPercent%")
        }

        val drillBonusPercent = (drillMultiplier - 1.0) * 100.0
        if (drillBonusPercent > 0.0) {
            lines.add("§7 Drill: §a+${drillBonusPercent.formatPretty()}%")
        }

        val petBonusPercent = (petMultiplier - 1.0) * 100.0
        if (petBonusPercent > 0.0 && petName != null) {
            lines.add("§7 $petName Pet: §a+${petBonusPercent.formatPretty()}%")
        }

        lines.add("")
        lines.add("§7Total Multiplier: §6${totalMultiplier.formatPretty()}x")
        lines.add("§7Effective Powder: §d${effectiveAmount.addSeparators()}")

        return lines
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        PowderChestReward.GEMSTONE_POWDER.chatPattern.matchMatcher(event.message) {
            val amountStr = groupOrNull("amount") ?: return
            val originalAmount = amountStr.formatInt()

            val breakdown = calculateMultiplierBreakdown()

            val effectiveAmount = (originalAmount * breakdown.totalMultiplier).toInt()

            ChatUtils.debug("[PowderDebug] Base: $originalAmount, Effective: $effectiveAmount, Multiplier: ${breakdown.totalMultiplier}")

            if (breakdown.totalMultiplier > 1.0) {
                val originalFormatted = originalAmount.addSeparators()
                val effectiveFormatted = effectiveAmount.addSeparators()

                val hoverText = breakdown.toTooltipLines(originalAmount, effectiveAmount)

                event.chatComponent = TextHelper.text("    §r§dGemstone Powder §r§8x$originalFormatted §7(x$effectiveFormatted)") {
                    this.hover = TextHelper.multiline(hoverText)
                }
            }
        }
    }


    private fun getDrillMultiplierFraction(): Double {
        var bonusPercent = 0
        val heldItem = InventoryUtils.getItemInHand() ?: return 1.0
        if(heldItem.getItemCategoryOrNull() != ItemCategory.DRILL) return 1.0

        val lore = heldItem.getLore()

        lore.forEach { line ->
            drillBasePowderPattern.matchMatcher(line) {
                bonusPercent += group("bonus").toInt()
            }
            drillUpgradePowderPattern.matchMatcher(line) {
                bonusPercent += group("bonus").toInt()
            }
        }
        val multiplier = 1.0 + (bonusPercent / 100.0)
        ChatUtils.debug("[PowderDebug] Drill multiplier: $multiplier")
        return multiplier
    }


    private fun getPetMultiplierFraction(): Double {
        var bonusPercent = 0.0
        val pet = CurrentPetApi.currentPet ?: return 1.0

        if (pet.cleanName == "Scatha" && pet.rarity == LorenzRarity.LEGENDARY) {
            bonusPercent = 0.2 * pet.level
        }

        val multiplier = 1.0 + (bonusPercent / 100.0)
        ChatUtils.debug("[PowderDebug] Pet multiplier: $multiplier")
        return multiplier
    }

    private fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.showEffectivePowder
}
