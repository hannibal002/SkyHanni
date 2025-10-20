package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import com.google.gson.annotations.Expose

// todo move back to TreeGiftTracker when 1.8 is no longer supported
@SkyHanniModule
@Suppress("unused")
object ForagingTrackerLegacy {

    enum class TreeType(private val displayName: String) {
        FIG("Fig"),
        MANGROVE("Mangrove"),
        ;

        override fun toString() = displayName

        fun getBaseLog() = internalNameCache.getOrPut((this to false)) { "${name}_LOG".toInternalName() }
        fun getEnchantedLog() = internalNameCache.getOrPut((this to true)) { "ENCHANTED_${name}_LOG".toInternalName() }

        companion object {
            private val internalNameCache: MutableMap<Pair<TreeType, Boolean>, NeuInternalName> = mutableMapOf()
            fun byNameOrNull(name: String): TreeType? = TreeType.entries.find {
                it.name.equals(name, ignoreCase = true)
            }
        }
    }

    data class BucketData(
        @Expose var treesCut: MutableMap<TreeType, Long> = enumMapOf(),
        @Expose var wholeTreesCut: MutableMap<TreeType, Double> = enumMapOf(),
        @Expose var hotfExperience: MutableMap<TreeType, Long> = enumMapOf(),
        @Expose var foragingExperience: MutableMap<TreeType, Long> = enumMapOf(),
        @Expose var forestWhispers: MutableMap<TreeType, Long> = enumMapOf(),
    ) : BucketedItemTrackerData<TreeType>(TreeType::class) {
        override fun getDescription(bucket: TreeType?, timesGained: Long): List<String> {
            val divisor = 1.coerceAtLeast(
                selectedBucket?.let {
                    treesCut[it]?.toInt()
                } ?: treesCut.sumAllValues().toInt(),
            )
            val percentage = timesGained.toDouble() / divisor
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCoinName(bucket: TreeType?, item: TrackedItem) = "§6Coins"
        override fun getCoinDescription(bucket: TreeType?, item: TrackedItem): List<String> {
            val mobKillCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7Cutting trees gives you coins.",
                "§7You got §6$mobKillCoinsFormat coins §7that way.",
            )
        }
        override fun TreeType.isBucketSelectable() = true
        override fun bucketName(): String = "tree"

        fun getTreeCount(): Long = selectedBucket?.let { treesCut[it] } ?: treesCut.values.sum()
        fun getWholeTreeCount(): Double = selectedBucket?.let { wholeTreesCut[it] } ?: wholeTreesCut.values.sum()
        fun getHotfExperience(): Long = selectedBucket?.let { hotfExperience[it] } ?: hotfExperience.values.sum()
        fun getForagingExperience(): Long = selectedBucket?.let { foragingExperience[it] } ?: foragingExperience.values.sum()
        fun getForestWhispers(): Long = selectedBucket?.let { forestWhispers[it] } ?: forestWhispers.values.sum()
    }

    private val patternGroup = RepoPattern.group("foraging.treegift")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §2§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    val openCloseRewardPattern by patternGroup.pattern(
        "open-close-reward",
        "§2§l▬{64}"
    )

    /**
     * REGEX-TEST:                                 §r§9§lTREE GIFT
     */
    val giftHeaderPattern by patternGroup.pattern(
        "header",
        " *(?:§.)+TREE GIFT"
    )

    /**
     * REGEX-TEST:                  §r§7You helped cut §r§a100% §r§7of the §r§aFig Tree§r§7.
     * REGEX-TEST:              §r§7You helped cut §r§a100% §r§7of the §r§aMangrove Tree§r§7.
     * REGEX-TEST:                  §r§7You helped cut §r§c15.2% §r§7of the §r§aFig Tree§r§7.
     */
    val percentageContributedPattern by patternGroup.pattern(
        "contribution-percentage",
        " *(?:§.)+You helped cut (?<percentColor>§.)+(?<percentage>[\\d.]+)% (?:§.)+of the (?:§.)+(?<type>.*) Tree(?:§.)+\\."
    )

    /**
     * REGEX-TEST: §f                       §e+5 rewards gained! §8(hover)
     * REGEX-TEST:                             §r§e+0 rewards gained!
     */
    val rewardsGainedPattern by patternGroup.pattern(
        "rewards-gained",
        "(?:§.)* *(?:§.)+\\+(?<count>[\\d,]+) rewards gained!(?: (?:§.)+\\(hover\\))?"
    )

    /**
     * REGEX-TEST: §2Forest Essence§r§8 x4
     * REGEX-TEST: §2Forest Essence§r§8 x12
     * REGEX-TEST: §2Forest Whispers §r§8x40
     * REGEX-TEST: §2Forest Whispers §r§8x100
     * REGEX-TEST: §3Foraging Experience §r§8x1,000
     * REGEX-TEST: §aHOTF Experience §8x10
     * REGEX-TEST: §aTender Wood §r§8x0-2
     * REGEX-TEST: §aVinesap §8x0-3
     * REGEX-TEST: §6Signal Enhancer §8(§a0.4%§8)
     */
    val hoverRewardPattern by patternGroup.pattern(
        "hover-reward",
        "(?:§.)*(?<item>[^§]+)\\s*(?:§.)*\\s*(?:§.)*§8\\s*x?(?:(?:0-)?(?<amount>[\\d,]+)|\\((?:§.)*(?<percentage>[\\d.]+)%(?:§.)*\\))"
    )

    /**
     * REGEX-TEST:                                 §r§d§lBONUS GIFT
     */
    val bonusGiftSeparatorPattern by patternGroup.pattern(
        "bonus-gift.separator",
        " *(?:§.)+BONUS GIFT"
    )

    /**
     * REGEX-TEST:                           §r§7§r§aStretching Sticks §r§8(§r§a20%§r§8)
     * REGEX-TEST:           §r§7§r§aEnchanted Book (§r§d§lFirst Impression I§r§a) §r§8(§r§a0.4%§r§8)
     * REGEX-TEST:           §r§7§r§aEnchanted Book (§r§d§lFirst Impression I§r§a) §r§8(§r§a0.4%§r§8)
     * REGEX-TEST:                            §r§7§r§fSweep Booster §r§8(§r§a1%§r§8)
     * REGEX-TEST:                     §r§7§r§fForaging Wisdom Booster §r§8(§r§a0.5%§r§8)
     * REGEX-TEST:                   §r§7§r§aEnchanted Book (§r§d§lMissile I§r§a) §r§8(§r§a0.2%§r§8)
     * REGEX-TEST:                           §r§7§r§cTree the Fish §r§8(§r§a0.05%§r§8)
     * REGEX-TEST:                             §r§6Chameleon §r§8(§r§a0.08%§r§8)
     * REGEX-FAIL:                      §r§7A §r§dPhanflare §r§7fell from the Tree!
     */
    val bonusGiftRewardPattern by patternGroup.pattern(
        "bonus-gift.reward",
        " *(?:§.)*§r(?<item>.*) §r§8\\((?:§.)+(?<percentage>[\\d.]+)%(?:§.)+\\)"
    )

    /**
     * REGEX-TEST: §aEnchanted Book (§r§d§lMissile I§r§a)
     * REGEX-TEST: §aEnchanted Book (§r§d§lFirst Impression I§r§a)
     */
    val enchantedBookPattern by patternGroup.pattern(
        "bonus-gift.enchanted-book",
        "(?:§.)+Enchanted Book \\((?:§.)+(?<book>.*) (?<tier>[IVCLX])(?:§.)+\\)"
    )

    /**
     * REGEX-TEST: ENCHANTED_FIG_LOG
     * REGEX-TEST: FIG_LOG
     * REGEX-TEST: ENCHANTED_MANGROVE_LOG
     * REGEX-TEST: MANGROVE_LOG
     */
    val logInternalNamePattern by patternGroup.pattern(
        "log-internal-name",
        "(?<enchanted>ENCHANTED_)?(?<treeType>.*)_LOG"
    )
    // </editor-fold>
}
