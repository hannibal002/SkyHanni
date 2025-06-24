package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.text.Text

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

    init {
        tracker.initRenderer({ config.position }) { isEnabled() }
    }

    enum class TreeType(private val displayName: String) {
        FIG("Fig"),
        MANGROVE("Mangrove"),
        ;

        override fun toString() = displayName

        companion object {
            fun byNameOrNull(name: String): TreeType? = TreeType.entries.find {
                it.name.equals(name, ignoreCase = true)
            }
        }
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
     * REGEX-TEST:                                 §r§9§lTREE GIFT
     */
    private val giftHeaderPattern by patternGroup.pattern(
        "header",
        " *(?:§.)+TREE GIFT"
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
     * REGEX-TEST: §2Forest Essence §8x4
     * REGEX-TEST: §2Forest Essence §8x6
     * REGEX-TEST: §2Forest Essence §8x12
     * REGEX-TEST: §2Forest Essence §8x16
     * REGEX-TEST: §2Forest Whispers §8x40
     * REGEX-TEST: §2Forest Whispers §8x60
     * REGEX-TEST: §2Forest Whispers §8x100
     * REGEX-TEST: §2Forest Whispers §8x160
     * REGEX-TEST: §3Foraging Experience §8x1,000
     * REGEX-TEST: §3Foraging Experience §8x2,000
     * REGEX-TEST: §3Foraging Experience §8x2,500
     * REGEX-TEST: §3Foraging Experience §8x5,000
     * REGEX-TEST: §3Foraging Experience §8x8,000
     * REGEX-TEST: §aHOTF Experience §8x10
     * REGEX-TEST: §aHOTF Experience §8x30
     * REGEX-TEST: §aHOTF Experience §8x50
     * REGEX-TEST: §aHOTF Experience §8x80
     * REGEX-TEST: §aTender Wood §8x0-2
     * REGEX-TEST: §aTender Wood §8x0-3
     * REGEX-TEST: §aTender Wood §8x0-5
     * REGEX-TEST: §aTender Wood §8x0-9
     * REGEX-TEST: §aVinesap §8x0-3
     */
    private val hoverRewardPattern by patternGroup.pattern(
        "hover-reward",
        "(?:§.)+(?<item>.*) §8x(?<amount>[\\d,-]+)"
    )

    /**
     * REGEX-TEST:                                 §r§d§lBONUS GIFT
     */
    private val bonusGiftSeparatorPattern by patternGroup.pattern(
        "bonus-gift.separator",
        " *(?:§.)+BONUS GIFT"
    )

    /**
     * REGEX-TEST:
     */
    private val bonusGiftRewardPattern by patternGroup.pattern(
        "bonus-gift.reward",
        ""
    )
    // </editor-fold>

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {

    }

    private fun isEnabled() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny() && heldItemEnabled() && !PlatformUtils.IS_LEGACY
    private fun heldItemEnabled() = !config.onlyHoldingAxe || isHoldingAxe()
    private fun isHoldingAxe() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.AXE

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled() || event.source != ItemAddManager.Source.COMMAND) return
        with(tracker) {
            event.addItemFromEvent()
        }
    }

    // Chat FSM
    private var openLootLoop = false
    private var openBonusGiftLoop = false
    private val loot = mutableMapOf<NeuInternalName, Int>()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onChat(event: SkyHanniChatEvent) {
        event.tryReadLoot()
        event.tryBlock()
    }

    private fun SkyHanniChatEvent.tryReadLoot() {
        openCloseRewardPattern.matchMatcher(message) {
            openLootLoop = !openLootLoop
        }
        if (!openLootLoop) return
    }

    private fun SkyHanniChatEvent.tryBlock() {
        if (!config.hideChats || !openLootLoop) return

    }

    private fun SkyHanniChatEvent.getLootPairs(): Set<Pair<NeuInternalName, Int>> {
        return if (rewardsGainedPattern.matches(message)) {
            chatComponent.getHoverLootPairs()
        } else setOf()
    }

    private fun Text.getHoverLootPairs(): Set<Pair<NeuInternalName, Int>> = buildSet {
        val joinedLines = hover.formattedTextCompat() + hover?.siblings?.joinToString {
            it.formattedTextCompat()
        }
        val splitLines = joinedLines.split("\n").takeIfNotEmpty() ?: return emptySet()
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
