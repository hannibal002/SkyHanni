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
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import com.google.gson.annotations.Expose

@SkyHanniModule
object TreeGiftTracker {

    enum class TreeType(private val displayName: String) {
        FIG("Fig"),
        MANGROVE("Mangrove"),
        HELIX("Helix"),
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
    ) : BucketedItemTrackerData<TreeType, SessionUptime.Normal>(TreeType::class, SessionUptime.Normal::class) {
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
     * REGEX-TEST: ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    val openCloseRewardPattern by patternGroup.pattern(
        "open-close-reward.colorless",
        "(?<line>▬{64})",
    )

    /**
     * WRAPPED-REGEX-TEST: "                                TREE GIFT"
     */
    val giftHeaderPattern by patternGroup.pattern(
        "header.colorless",
        " *TREE GIFT",
    )

    /**
     * WRAPPED-REGEX-TEST: "                 You helped cut 100% of the Fig Tree."
     * WRAPPED-REGEX-TEST: "             You helped cut 100% of the Mangrove Tree."
     * WRAPPED-REGEX-TEST: "                 You helped cut 15.2% of the Fig Tree."
     */
    val percentageContributedPattern by patternGroup.pattern(
        "contribution-percentage.colorless",
        """ *You helped cut (?<percentage>[\d.]+)% +of the (?<type>\w+) Tree\.""",
    )

    /**
     * WRAPPED-REGEX-TEST: "                       +5 rewards gained! (hover)"
     * WRAPPED-REGEX-TEST: "                            +0 rewards gained!"
     */
    val rewardsGainedPattern by patternGroup.pattern(
        "rewards-gained.colorless",
        """ *\+(?<count>[\d,]+) rewards gained!(?: \(hover\))?""",
    )

    /**
     * REGEX-TEST: Forest Essence x4
     * REGEX-TEST: Forest Essence x12
     * REGEX-TEST: Forest Whispers x40
     * REGEX-TEST: Forest Whispers x100
     * REGEX-TEST: Foraging Experience x1,000
     * REGEX-TEST: HOTF Experience x10
     * REGEX-TEST: Tender Wood x0-2
     * REGEX-TEST: Vinesap x0-3
     * REGEX-TEST: Signal Enhancer (0.4%)
     */
    @Suppress("MaxLineLength")
    val hoverRewardPattern by patternGroup.pattern(
        "hover-reward.colorless",
        """(?<item>\S(?:.*\S)?)\s*x?(?:(?:0-)?(?<amount>[\d,]+)|\((?<percentage>[\d.]+)%\))""",
    )

    /**
     * WRAPPED-REGEX-TEST: "                                BONUS GIFT"
     */
    val bonusGiftSeparatorPattern by patternGroup.pattern(
        "bonus-gift.separator.colorless",
        " *BONUS GIFT",
    )

    /**
     * WRAPPED-REGEX-TEST: "                          Stretching Sticks (20%)"
     * WRAPPED-REGEX-TEST: "          Enchanted Book (First Impression I) (0.4%)"
     * WRAPPED-REGEX-TEST: "          Enchanted Book (First Impression I) (0.4%)"
     * WRAPPED-REGEX-TEST: "                           Sweep Booster (1%)"
     * WRAPPED-REGEX-TEST: "                    Foraging Wisdom Booster (0.5%)"
     * WRAPPED-REGEX-TEST: "                  Enchanted Book (Missile I) (0.2%)"
     * WRAPPED-REGEX-TEST: "                          Tree the Fish (0.05%)"
     * WRAPPED-REGEX-TEST: "                            Chameleon (0.08%)"
     * WRAPPED-REGEX-TEST: "                    Enchanted Book (Karma I) (0.02%)"
     * WRAPPED-REGEX-FAIL: "                     A Phanflare fell from the Tree!"
     */
    val bonusGiftRewardPattern by patternGroup.pattern(
        "bonus-gift.reward.colorless",
        """ *(?<item>.+) \((?<percentage>[\d.]+)%\)""",
    )

    /**
     * REGEX-TEST: Enchanted Book (Missile I)
     * REGEX-TEST: Enchanted Book (First Impression I)
     * REGEX-TEST: Enchanted Book (Karma I)
     */
    val enchantedBookPattern by patternGroup.pattern(
        "bonus-gift.enchanted-book.colorless",
        """ *Enchanted Book \((?<book>.+) (?<tier>[IVCLX]+)\)""",
    )

    /**
     * REGEX-TEST: A Phanpyre fell from the Tree!
     * REGEX-TEST: A Phanflare fell from the Tree!
     * REGEX-TEST: A Dreadwing fell from the Tree!
     * REGEX-TEST: A Firefox fell from the Tree!
     * REGEX-TEST: A Grizzly Bear fell from the Tree!
     */
    val mobSpawnPattern by patternGroup.pattern(
        "bonus-gift.mob",
        """ *+A (?<mob>[\w ]+) fell from the Tree!""",
    )

    /**
     * REGEX-TEST: ENCHANTED_FIG_LOG
     * REGEX-TEST: FIG_LOG
     * REGEX-TEST: ENCHANTED_MANGROVE_LOG
     * REGEX-TEST: MANGROVE_LOG
     */
    val logInternalNamePattern by patternGroup.pattern(
        "log-internal-name",
        "(?<enchanted>ENCHANTED_)?(?<treeType>.*)_LOG",
    )
    // </editor-fold>
}
