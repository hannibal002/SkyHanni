package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.foraging.ForagingTrackerConfig
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.TreeGiftBonusDropsJson
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.foraging.ForagingTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import kotlin.time.Duration.Companion.seconds

private typealias DropCategory = ForagingTrackerConfig.TreeGiftBonusDropCategory

@SkyHanniModule
object ForagingTracker : SkyHanniBucketedItemTracker<ForagingTracker.TreeType, ForagingTracker.BucketData>(
    "Foraging Tracker",
    { BucketData() },
    { it.foraging.trackerData },
    { drawDisplay(it) },
    trackerConfig = { SkyHanniMod.feature.foraging.tracker.perTrackerConfig },
) {
    private val config get() = SkyHanniMod.feature.foraging.tracker

    init {
        initRenderer({ config.position }) { isInIsland() && heldItemEnabled() && config.enabled }
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

    private fun heldItemEnabled() = !config.onlyHoldingAxe ||
        (isHoldingAxe() || lastAxeHeldTime.passedSince() < config.disappearingDelay.seconds)

    private fun isHoldingAxe() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.AXE || hasHeldAxe

    private var lastAxeHeldTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var hasHeldAxe: Boolean = false

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {
        addSearchString("§a§lForaging Tracker")
        addBucketSelector(this, bucketData, "Tree Type")

        val treesContributedTo = bucketData.getTreeCount()
        if (treesContributedTo == 0L) return@buildList

        val profit = drawItems(bucketData, { true }, this)

        val foragingXp = bucketData.getForagingExperience()
        if (foragingXp > 0) addSearchString("§eForaging Experience: §3${foragingXp.addSeparators()}")

        val hotfXp = bucketData.getHotfExperience()
        if (hotfXp > 0) addSearchString("§eHOTF Experience: §a${hotfXp.addSeparators()}")

        val forestWhispers = bucketData.getForestWhispers()
        if (forestWhispers > 0) addSearchString("§eForest Whispers: §b${forestWhispers.addSeparators()}")

        val bucketFormat = bucketData.selectedBucket?.let { "$it " }.orEmpty()
        val baseFormat = "${bucketFormat}Trees Felled:"

        val wholeTreesFelled = bucketData.getWholeTreeCount()
        if (config.showWholeTrees && wholeTreesFelled > 0.0) {
            val preambleFormat = "Whole $baseFormat"
            val wholeRenderable = Renderable.hoverTips(
                Renderable.text("§e$preambleFormat ${wholeTreesFelled.addSeparators()}"),
                tips = bucketData.wholeTreesCut.mapNotNull { (treeType, count) ->
                    if (count <= 0.0) return@mapNotNull null
                    "§7Whole $treeType Trees cut: §a${count.addSeparators()}"
                },
            ).toSearchable("whole trees felled")
            add(wholeRenderable)
        }

        val totalRenderable = Renderable.hoverTips(
            Renderable.text("§e$baseFormat ${treesContributedTo.addSeparators()}"),
            tips = bucketData.treesCut.mapNotNull { (treeType, count) ->
                if (count <= 0) return@mapNotNull null
                "$treeType Tree contributions: §a${count.addSeparators()}"
            },
        ).toSearchable("trees felled")
        add(totalRenderable)

        val duration = bucketData.getTotalUptime()
        addAll(addTotalProfit(profit, treesContributedTo, "gift", duration, "Gifts"))
        addPriceFromButton(this)
    }

    @HandleEvent
    private fun onItemAdd(event: ItemAddEvent) {
        if (!isInIsland() || event.source != ItemAddManager.Source.COMMAND) return
        event.addItemFromEvent()
    }

    @HandleEvent
    private fun onSackChange(event: SackChangeEvent) {
        if (!isInIsland()) return
        event.addLogs()
    }

    private data class LogSackChange(
        val treeType: TreeType,
        val delta: Int,
        val deltaEnchanted: Int,
    )

    private fun SackChangeEvent.addLogs() = extractLogs().groupBy { it.treeType }.mapValues { (_, changes) ->
        changes.fold(LogSackChange(changes.first().treeType, 0, 0)) { acc, change ->
            LogSackChange(
                change.treeType,
                acc.delta + change.delta,
                acc.deltaEnchanted + change.deltaEnchanted,
            )
        }
    }.values.forEach { (treeType, delta, deltaEnchanted) ->
        val baseLog = treeType.getBaseLog()
        if (delta > 0) addItem(treeType, baseLog, delta, command = false)

        val enchantedLog = treeType.getEnchantedLog()
        if (deltaEnchanted > 0) addItem(treeType, enchantedLog, deltaEnchanted, command = false)
    }

    private fun SackChangeEvent.extractLogs(): List<LogSackChange> = sackChanges.asSequence()
        .filter { it.delta > 0 }.mapNotNull { change ->
            logInternalNamePattern.matchMatcher(change.internalName.asString()) {
                val type = TreeType.byNameOrNull(group("treeType"))
                    ?: return@matchMatcher null
                val enchanted = groupOrNull("enchanted") != null
                LogSackChange(
                    type,
                    if (enchanted) 0 else change.delta,
                    if (enchanted) change.delta else 0,
                )
            }
        }.toList()

    // Chat FSM
    private var openLootLoop = false
    private var openBonusGiftLoop = false
    private var treeType: TreeType? = null
    private var lastTreeGiftAt: SimpleTimeMark = SimpleTimeMark.farPast()
    private val loot = mutableMapOf<NeuInternalName, Int>()

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isInIsland()) return
        event.tryReadLoot()
        event.tryBlock()
    }

    private val STRETCHING_STICKS = "STRETCHING_STICKS".toInternalName()
    private var currentStretchingSticks = 0

    @HandleEvent(OwnInventoryItemUpdateEvent::class)
    private fun onOwnInventoryItemUpdate() {
        if (!isInIsland()) return
        val treeType = treeType ?: return

        val stretchingSticksNow = InventoryUtils.getItemsInOwnInventory().filter {
            it.getInternalNameOrNull() == STRETCHING_STICKS
        }.sumOf { it.count }

        val change = stretchingSticksNow - currentStretchingSticks
        currentStretchingSticks = stretchingSticksNow
        if (change <= 0) return
        addItem(treeType, STRETCHING_STICKS, change, command = false)
    }

    private data class DropCategoryData(
        val category: DropCategory,
        val items: List<NeuInternalName>,
    )

    private var dropsJson: TreeGiftBonusDropsJson? = null
    private var dropsJsonCategories: List<DropCategoryData> = emptyList()

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        dropsJson = event.getConstant<TreeGiftBonusDropsJson>("foraging/TreeGiftBonusDrops")
        val dropsJson = dropsJson ?: return
        dropsJsonCategories = buildList {
            add(DropCategoryData(DropCategory.UNCOMMON_DROPS, dropsJson.uncommonDrops))
            add(DropCategoryData(DropCategory.ENCHANTED_BOOKS, dropsJson.enchantedBooks))
            add(DropCategoryData(DropCategory.BOOSTERS, dropsJson.boosters))
            add(DropCategoryData(DropCategory.SHARDS, dropsJson.shards))
            add(DropCategoryData(DropCategory.RUNES, dropsJson.runes))
            add(DropCategoryData(DropCategory.MISC, dropsJson.miscDrops))
        }
    }

    // TODO split up
    @Suppress("CyclomaticComplexMethod")
    private fun SkyHanniChatEvent.Allow.tryReadLoot() {
        val dropsJson = dropsJson ?: return

        openCloseRewardPattern.matchStyledMatcher(chatComponent) {
            val style = groupOrThrow("line").sampleStyleAtStart()
            if (!style.isBold) return@matchStyledMatcher
            if (style.color != TextColor.fromLegacyFormat(DARK_GREEN)) return@matchStyledMatcher

            openLootLoop = !openLootLoop
            if (openLootLoop) {
                openBonusGiftLoop = false
                lastTreeGiftAt = SimpleTimeMark.now()
            } else {
                sendTreeGiftStats()
                val treeType = treeType ?: FIG
                loot.forEach { (item, count) ->
                    addItem(treeType, item, count, command = false)
                }
                loot.clear()
            }
            if (config.compactGiftChats) blockedReason = "TREE_GIFT"
        }
        if (!openLootLoop) return

        bonusGiftSeparatorPattern.matchMatcher(cleanMessage) {
            openBonusGiftLoop = true
            return
        }

        percentageContributedPattern.matchMatcher(cleanMessage) {
            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            val percentColor = when (percentage) {
                in 0.0..<20.0 -> 'c'
                in 20.0..<33.0 -> 'e'
                in 33.0..100.0 -> 'a'
                else -> error("Invalid tree cut percentage: $percentage")
            }
            lastPercentString = "§$percentColor$percentage%"
            val type = group("type")
            treeType = TreeType.byNameOrNull(type)
            val treeType = treeType ?: return@matchMatcher
            modify {
                it.treesCut.addOrPut(treeType, 1)
                it.wholeTreesCut.addOrPut(treeType, percentage / 100.0)
            }
        }

        rewardsGainedPattern.matchMatcher(cleanMessage) {
            group("count").formatIntOrNull()?.let { lastRewardCount = it }
            val dataSibling = chatComponent.siblings.firstOrNull() ?: return@matchMatcher
            dataSibling.getHoverLootPairs().forEach { (item, amount) ->
                loot.addOrPut(item, amount)
            }
        }

        mobSpawnPattern.matchMatcher(cleanMessage) {
            val mob = group("mob")

            if (dropsJson.mobs.contains(mob) && config.compactGiftBonusDropsList.contains(DropCategory.MOBS)) {
                rareDrops.add("A wild §d$mob §fappeared!")
            }
        }

        if (!openBonusGiftLoop) return

        val item = bonusGiftRewardPattern.matchStyledMatcher(chatComponent) {
            componentOrThrow("item")
        } ?: return
        var itemInternalName = enchantedBookPattern.matchStyledMatcher(item) {
            val book = componentOrThrow("book").formattedTextCompatLeadingWhiteLessResets()
            val tier = groupOrThrow("tier").getText().romanToDecimal()
            NeuInternalName.fromItemNameOrNull("$book $tier")
        } ?: NeuInternalName.fromItemNameOrNull(item) ?: return

        // this is a failsafe in the event of runes lacking sufficient NEU repo data to automagically
        // fetch their correct internal names, and thus translating their in-game names into internal
        // names literally
        if (itemInternalName.startsWith(("◆_"))) itemInternalName = itemInternalName.replace("◆_", "AXE_")

        loot.addOrPut(itemInternalName, 1)

        val bonusDropTypeList = config.compactGiftBonusDropsList
        val inCategoryList = dropsJsonCategories.any {
            it.category in bonusDropTypeList && it.items.contains(itemInternalName)
        }
        if (inCategoryList) rareDrops.add(item.formattedTextCompat())
    }

    private fun SkyHanniChatEvent.Allow.tryBlock() {
        if (!config.compactGiftChats || !openLootLoop) return
        blockedReason = "TREE_GIFT"
    }

    private fun Component.getHoverLootPairs(): Set<Pair<NeuInternalName, Int>> = buildSet {
        val treeType = treeType ?: return this
        lastHover = hover
        val lootLines = hover?.string?.split("\n")?.takeIfNotEmpty() ?: return this
        ChatUtils.debug("found loot lines:\n${lootLines.joinToString("\n")}")
        lootLines.forEach { line ->
            val (item, amountString) = hoverRewardPattern.matchMatcher(line) {
                val amountString = if (groupOrNull("percentage") != null) "1" else group("amount")
                group("item") to amountString
            } ?: return@forEach
            ChatUtils.debug("found hover loot: $item x$amountString")
            val amount = amountString.formatIntOrNull() ?: return@forEach
            when (item) {
                "HOTF Experience" -> modify {
                    it.hotfExperience.addOrPut(treeType, amount.toLong())
                }

                "Foraging Experience" -> modify {
                    it.foragingExperience.addOrPut(treeType, amount.toLong())
                }

                "Forest Whispers" -> modify {
                    it.forestWhispers.addOrPut(treeType, amount.toLong())
                }

                else -> NeuInternalName.fromItemNameOrNull(item)?.let {
                    ChatUtils.debug("Adding hover loot: $it x$amount")
                    add(it to amount)
                }
            }
        }
    }

    private var lastPercentString = ""
    private var lastRewardCount = 0
    private val rareDrops = mutableListOf<String>()
    private var lastHover: Component? = null

    private fun sendTreeGiftStats() {
        val lastTreeType = treeType ?: return
        if (config.compactGiftChats) {
            val message = "§9$lastTreeType Tree Gift. §7You helped cut $lastPercentString §7and gained §e$lastRewardCount rewards§a!"
            val component = message.asComponent()
            component.hover = lastHover
            ChatUtils.chat(component, prefix = false)
            rareDrops.forEach { drop ->
                ChatUtils.chat("§f - $drop", prefix = false)
            }
        }
        rareDrops.clear()
        lastHover = null
    }

    @HandleEvent
    private fun onIslandLeave() {
        if (!isInIsland()) return
        firstUpdate()
    }

    private fun isInIsland() = IslandTypeTag.FORAGING_CUSTOM_TREES.isInIsland()

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetforagingtracker") {
            description = "Resets the Foraging Tracker."
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }

    @HandleEvent
    private fun onItemInHandChange(event: ItemInHandChangeEvent) {
        if (!isInIsland()) return
        val isAxe = event.newItem.getItemStack().getItemCategoryOrNull() == ItemCategory.AXE
        if (isAxe != hasHeldAxe) {
            if (!isAxe) {
                lastAxeHeldTime = SimpleTimeMark.now()
            }
            hasHeldAxe = isAxe
        }
    }

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
}
