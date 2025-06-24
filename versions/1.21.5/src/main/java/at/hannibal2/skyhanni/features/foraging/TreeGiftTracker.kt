package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose

@SkyHanniModule
object TreeGiftTracker {

    private val config get() = SkyHanniMod.feature.foraging.treeGiftTracker
    private val patternGroup = RepoPattern.group("foraging.treegift")

    private val tracker = SkyHanniBucketedItemTracker(
        "Tree Gift Tracker",
        { BucketData() },
        { it.foraging.treeGiftTracker },
        { drawDisplay(it) }
    )

    enum class TreeType(private val displayName: String) {
        FIG("Fig"),
        MANGROVE("Mangrove"),
        ;

        override fun toString() = displayName
    }

    class BucketData : BucketedItemTrackerData<TreeType>(TreeType::class) {
        override fun resetItems() {
            treesCut = enumMapOf()
        }

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

        override fun getCoinName(bucket: TreeType?, item: TrackedItem) = "<no coins>"
        override fun getCoinDescription(bucket: TreeType?, item: TrackedItem): List<String> = listOf("<no coins>")

        override fun TreeType.isBucketSelectable() = true

        @Expose
        var treesCut: MutableMap<TreeType, Long> = enumMapOf()

        @Expose
        var totalTreesCut: MutableMap<TreeType, Double> = enumMapOf()
    }

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §9§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val openCloseRewardPattern by patternGroup.pattern(
        "open-close-reward",
        "§9§l▬{64}"
    )

    /**
     * REGEX-TEST:                  §r§7You helped cut §r§a100% §r§7of the §r§aFig Tree§r§7.
     * REGEX-TEST:              §r§7You helped cut §r§a100% §r§7of the §r§aMangrove Tree§r§7.
     * REGEX-TEST:                  §r§7You helped cut §r§c15.2% §r§7of the §r§aFig Tree§r§7.
     */
    private val percentageContributedPattern by patternGroup.pattern(
        "contribution-percentage",
        " *(?:§.)+You helped cut (?:§.)+(?<percentage>[\\d.]+)% (?:§.)+of the (?:§.)+(?<type>.*) Tree(?:§.)+\\."
    )

    /**
     * REGEX-TEST: §f                       §e+5 rewards gained! §8(hover)
     */
    private val rewardsGainedPattern by patternGroup.pattern(
        "rewards-gained",
        "(?:§.)+ *(?:§.)+\\+(?<count>[\\d,]+) rewards gained! (?:§.)+\\(hover\\)"
    )

    /**
     * REGEX-TEST:                                 §r§d§lBONUS GIFT
     */
    private val bonusGiftSeparatorPattern by patternGroup.pattern(
        "bonus-gift-separator",
        " *(?:§.)+BONUS GIFT"
    )
    // </editor-fold>

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {

    }

    private fun isEnabled() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny() && heldItemEnabled()
    private fun heldItemEnabled() = !config.onlyHoldingAxe || isHoldingAxe()
    private fun isHoldingAxe() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.AXE

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onChat(event: SkyHanniChatEvent) {

    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return
        tracker.firstUpdate()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresettreegifttracker") {
            description = "Resets the Tree Gift Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }

}
