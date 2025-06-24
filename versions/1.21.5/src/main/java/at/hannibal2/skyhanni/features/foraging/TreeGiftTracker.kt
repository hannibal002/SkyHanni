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
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.text.Text
import kotlin.collections.emptySet
import kotlin.time.Duration.Companion.seconds

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
            wholeTreesCut = enumMapOf()
            hotfExperience = enumMapOf()
            foragingExperience = enumMapOf()
            forestWhispers = enumMapOf()
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

        fun getTreeCount(): Long = selectedBucket?.let { treesCut[it] } ?: treesCut.values.sum()

        @Expose
        var wholeTreesCut: MutableMap<TreeType, Double> = enumMapOf()

        fun getWholeTreeCount(): Double = selectedBucket?.let { wholeTreesCut[it] } ?: wholeTreesCut.values.sum()

        @Expose
        var hotfExperience: MutableMap<TreeType, Long> = enumMapOf()

        fun getHotfExperience(): Long = selectedBucket?.let { hotfExperience[it] } ?: hotfExperience.values.sum()

        @Expose
        var foragingExperience : MutableMap<TreeType, Long> = enumMapOf()

        fun getForagingExperience(): Long = selectedBucket?.let { foragingExperience[it] } ?: foragingExperience.values.sum()

        @Expose
        var forestWhispers: MutableMap<TreeType, Long> = enumMapOf()

        fun getForestWhispers(): Long = selectedBucket?.let { forestWhispers[it] } ?: forestWhispers.values.sum()
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
    private val bonusGiftRewardPattern by patternGroup.pattern(
        "bonus-gift.reward",
        " *(?:§.)*§r(?<item>.*) §r§8\\((?:§.)+(?<percentage>[\\d.]+)%(?:§.)+\\)"
    )

    /**
     * REGEX-TEST: §aEnchanted Book (§r§d§lMissile I§r§a)
     * REGEX-TEST: §aEnchanted Book (§r§d§lFirst Impression I§r§a)
     */
    private val enchantedBookPattern by patternGroup.pattern(
        "bonus-gift.enchanted-book",
        "(?:§.)+Enchanted Book \\((?:§.)+(?<book>.*) (?<tier>[IVCLX])(?:§.)+\\)"
    )
    // </editor-fold>

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {
        addSearchString("§a§lTree Gift Tracker")
        tracker.addBucketSelector(this, bucketData, "Tree Type")

        val treesContributedTo = bucketData.getTreeCount()
        if (treesContributedTo == 0L) return@buildList

        val profit = tracker.drawItems(bucketData, { true }, this)

        val foragingXp = bucketData.getForagingExperience()
        if (foragingXp > 0) addSearchString("§eForaging Experience: §3${foragingXp.addSeparators()}")

        val hotfXp = bucketData.getHotfExperience()
        if (hotfXp > 0) addSearchString("§eHOTF Experience: §a${hotfXp.addSeparators()}")

        val forestWhispers = bucketData.getForestWhispers()
        if (forestWhispers > 0) addSearchString("§eForest Whispers: §b${forestWhispers.addSeparators()}")

        val treeFormat = "Tree".pluralize(treesContributedTo.toInt())
        val bucketFormat = bucketData.selectedBucket?.let { "$it " }.orEmpty()
        val baseFormat = "${bucketFormat}$treeFormat Felled:"

        val wholeTreesFelled = bucketData.getWholeTreeCount()
        if (config.showWholeTrees && wholeTreesFelled > 0.0) {
            val preambleFormat = "Whole $baseFormat"
            addSearchString("§e$preambleFormat ${wholeTreesFelled.addSeparators()}")
        }

        addSearchString("§e$baseFormat ${treesContributedTo.addSeparators()}")
        add(tracker.addTotalProfit(profit, treesContributedTo, "gift"))
        tracker.addPriceFromButton(this)
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

    private val rangedItems: MutableSet<NeuInternalName> = mutableSetOf()

    @HandleEvent
    fun onSackChange(event: SackChangeEvent) {
        if (lastTreeGiftAt.passedSince() >= 30.seconds) return
        val lastTreeType = treeType ?: return
        event.sackChanges.filter {
            it.delta > 0 && it.internalName in rangedItems
        }.forEach {
            tracker.addItem(
                lastTreeType,
                it.internalName,
                it.delta,
                command = false
            )
        }
    }

    // Chat FSM
    private var openLootLoop = false
    private var openBonusGiftLoop = false
    private var treeType: TreeType? = null
    private var lastTreeGiftAt: SimpleTimeMark = SimpleTimeMark.farPast()
    private val loot = mutableMapOf<NeuInternalName, Int>()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onChat(event: SkyHanniChatEvent) {
        event.tryReadLoot()
        event.tryBlock()
    }

    private fun SkyHanniChatEvent.tryReadLoot() {
        openCloseRewardPattern.matchMatcher(message) {
            openLootLoop = !openLootLoop
            if (openLootLoop) {
                openBonusGiftLoop = false
                lastTreeGiftAt = SimpleTimeMark.now()
            }
            if (config.hideChats) blockedReason = "TREE_GIFT"
        }
        if (!openLootLoop) return

        bonusGiftSeparatorPattern.matchMatcher(message) {
            openBonusGiftLoop = true
            return
        }

        percentageContributedPattern.matchMatcher(message) {
            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            val type = group("type")
            treeType = TreeType.byNameOrNull(type)
            val treeType = treeType ?: return@matchMatcher
            tracker.modify {
                it.treesCut.addOrPut(treeType, 1)
                it.wholeTreesCut.addOrPut(treeType, percentage / 100.0)
            }
        }

        rewardsGainedPattern.matchMatcher(message) {
            chatComponent.getHoverLootPairs().forEach { (item, amount) ->
                loot.addOrPut(item, amount)
            }
        }

        if (!openBonusGiftLoop) return
        bonusGiftRewardPattern.matchMatcher(message) {
            val item = group("item")
            val itemInternalName = enchantedBookPattern.matchMatcher(item) {
                val book = group("book")
                val tier = group("tier").romanToDecimal()
                NeuInternalName.fromItemNameOrNull("$book $tier")
            } ?: NeuInternalName.fromItemNameOrNull(item) ?: return@matchMatcher
            loot.addOrPut(itemInternalName, 1)
        }
    }

    private fun SkyHanniChatEvent.tryBlock() {
        if (!config.hideChats || !openLootLoop) return
        blockedReason = "TREE_GIFT"
    }

    private fun Text.getHoverLootPairs(): Set<Pair<NeuInternalName, Int>> = buildSet {
        val treeType = treeType ?: return emptySet()
        val joinedLines = hover.formattedTextCompat() + hover?.siblings?.joinToString { it.formattedTextCompat() }
        joinedLines.split("\n").forEach { line ->
            val (item, amountString) = hoverRewardPattern.matchMatcher(line) {
                group("item") to group("amount")
            } ?: return@forEach
            if (amountString.contains("-")) {
                NeuInternalName.fromItemNameOrNull(item)?.let {
                    rangedItems.add(it)
                }
                return@forEach
            } // Skip ranges like "0-2" (for now)
            val amount = amountString.formatIntOrNull() ?: return@forEach
            when (item) {
                "HOTF Experience" -> return@forEach tracker.modify {
                    it.hotfExperience.addOrPut(treeType, amount.toLong())
                }
                "Foraging Experience" -> return@forEach tracker.modify {
                    it.foragingExperience.addOrPut(treeType, amount.toLong())
                }
                "Forest Whispers" -> return@forEach tracker.modify {
                    it.forestWhispers.addOrPut(treeType, amount.toLong())
                }
                else -> {
                    val itemInternalName = NeuInternalName.fromItemNameOrNull(item) ?: return@forEach
                    add(itemInternalName to amount)
                }
            }
        }
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
