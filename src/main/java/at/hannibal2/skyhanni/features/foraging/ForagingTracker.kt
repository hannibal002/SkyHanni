package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.foraging.ForagingTrackerConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.TreeGiftBonusDropsJson
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
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
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.data.BucketedItemTrackerData
import com.google.gson.annotations.Expose
import net.minecraft.network.chat.Component
import kotlin.time.Duration.Companion.seconds

private typealias DropCategory = ForagingTrackerConfig.TreeGiftBonusDropCategory

@SkyHanniModule
object ForagingTracker : SkyHanniBucketedItemTracker<TreeType, ForagingTracker.BucketData>(
    "Foraging Tracker",
) {
    override val storageAccessor: (ProfileSpecificStorage) -> BucketData = { it.foraging.trackerData }
    override val config get() = SkyHanniMod.feature.foraging.tracker
    override val renderConfig = RenderDisplayConfig(
        condition = { heldItemEnabled() && config.enabled },
        onlyOnIslandTag = IslandTypeTag.FORAGING_CUSTOM_TREES,
    )
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
    @Suppress("MaxLineLength")
    val hoverRewardPattern by patternGroup.pattern(
        "hover-reward",
        "(?:§.)*(?<item>[^§\\s](?:[^§]*[^§\\s])?)(?:§.)*\\s*(?:§.)*§8\\s*x?(?:(?:0-)?(?<amount>[\\d,]+)|\\((?:§.)*(?<percentage>[\\d.]+)%(?:§.)*\\))"
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
        " *(?:§.)+Enchanted Book \\((?:§.)+(?<book>.*) (?<tier>[IVCLX])(?:§.)+\\)"
    )

    /**
     * REGEX-TEST: §r§7A §r§dPhanpyre §r§7fell from the Tree!
     * REGEX-TEST: §r§7A §r§dPhanflare §r§7fell from the Tree!
     * REGEX-TEST: §r§7A §r§dDreadwing §r§7fell from the Tree!
     */
    val phantomSpawnPattern by patternGroup.pattern(
        "bonus-gift.phantoms",
        " *(?:§.)+A (?:§.)+(?<phantom>.*) (?:§.)+fell from the Tree!"
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

    data class BucketData(
        @Expose var treesCut: MutableMap<TreeType, Long> = CollectionUtils.enumMapOf(),
        @Expose var wholeTreesCut: MutableMap<TreeType, Double> = CollectionUtils.enumMapOf(),
        @Expose var hotfExperience: MutableMap<TreeType, Long> = CollectionUtils.enumMapOf(),
        @Expose var foragingExperience: MutableMap<TreeType, Long> = CollectionUtils.enumMapOf(),
        @Expose var forestWhispers: MutableMap<TreeType, Long> = CollectionUtils.enumMapOf(),
    ) : BucketedItemTrackerData<TreeType, SessionUptime.Normal>() {
        override fun getDescription(bucket: TreeType?, timesGained: Long): List<String> =
            super.getDropRate(treesCut, bucket, timesGained)

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

    private fun heldItemEnabled() = !config.onlyHoldingAxe ||
        (isHoldingAxe() || lastAxeHeldTime.passedSince() < config.disappearingDelay.seconds)

    private fun isHoldingAxe() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.AXE || hasHeldAxe

    private var lastAxeHeldTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var hasHeldAxe: Boolean = false

    override fun drawDisplayF(data: BucketData): List<Searchable> = buildList {
        addSearchString("§a§lForaging Tracker")
        addBucketSelector(this, data, "Tree Type")

        val treesContributedTo = data.getTreeCount()
        if (treesContributedTo == 0L) return@buildList

        val profit = drawItems(data, { true }, this)

        val foragingXp = data.getForagingExperience()
        if (foragingXp > 0) addSearchString("§eForaging Experience: §3${foragingXp.addSeparators()}")

        val hotfXp = data.getHotfExperience()
        if (hotfXp > 0) addSearchString("§eHOTF Experience: §a${hotfXp.addSeparators()}")

        val forestWhispers = data.getForestWhispers()
        if (forestWhispers > 0) addSearchString("§eForest Whispers: §b${forestWhispers.addSeparators()}")

        val bucketFormat = data.selectedBucket?.let { "$it " }.orEmpty()
        val baseFormat = "${bucketFormat}Trees Felled:"

        val wholeTreesFelled = data.getWholeTreeCount()
        if (config.showWholeTrees && wholeTreesFelled > 0.0) {
            val preambleFormat = "Whole $baseFormat"
            val wholeRenderable = Renderable.hoverTips(
                Renderable.text("§e$preambleFormat ${wholeTreesFelled.addSeparators()}"),
                tips = data.wholeTreesCut.mapNotNull { (treeType, count) ->
                    if (count <= 0.0) return@mapNotNull null
                    "§7Whole $treeType Trees cut: §a${count.addSeparators()}"
                },
            ).toSearchable("whole trees felled")
            add(wholeRenderable)
        }

        val totalRenderable = Renderable.hoverTips(
            Renderable.text("§e$baseFormat ${treesContributedTo.addSeparators()}"),
            tips = data.treesCut.mapNotNull { (treeType, count) ->
                if (count <= 0) return@mapNotNull null
                "$treeType Tree contributions: §a${count.addSeparators()}"
            },
        ).toSearchable("trees felled")
        add(totalRenderable)

        val duration = data.getTotalUptime()
        addAll(addTotalProfit(profit, treesContributedTo, "gift", duration, "Gifts"))
        addPriceFromButton(this)
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onItemAdd(event: ItemAddEvent) {
        if (event.source != ItemAddManager.Source.COMMAND) return
        event.addItemFromEvent()
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onSackChange(event: SackChangeEvent) {
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

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onChat(event: SkyHanniChatEvent.Allow) {
        event.tryReadLoot()
        event.tryBlock()
    }

    private val STRETCHING_STICKS = "STRETCHING_STICKS".toInternalName()
    private var currentStretchingSticks = 0

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onOwnInventoryItemUpdate() {
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
    fun onRepoReload(event: RepositoryReloadEvent) {
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

    private fun SkyHanniChatEvent.Allow.tryReadLoot() {
        val dropsJson = dropsJson ?: return

        openCloseRewardPattern.matchMatcher(message) {
            openLootLoop = !openLootLoop
            if (openLootLoop) {
                openBonusGiftLoop = false
                lastTreeGiftAt = SimpleTimeMark.now()
            } else {
                sendTreeGiftStats()
                val treeType = treeType ?: TreeType.FIG
                loot.forEach { (item, count) ->
                    addItem(treeType, item, count, command = false)
                }
                loot.clear()
            }
            if (config.compactGiftChats) blockedReason = "TREE_GIFT"
        }
        if (!openLootLoop) return

        bonusGiftSeparatorPattern.matchMatcher(message) {
            openBonusGiftLoop = true
            return
        }

        percentageContributedPattern.matchMatcher(message) {
            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            val percentColor = group("percentColor")
            lastPercentString = "$percentColor$percentage%"
            val type = group("type")
            treeType = TreeType.byNameOrNull(type)
            val treeType = treeType ?: return@matchMatcher
            modify {
                it.treesCut.addOrPut(treeType, 1)
                it.wholeTreesCut.addOrPut(treeType, percentage / 100.0)
            }
        }

        rewardsGainedPattern.matchMatcher(message) {
            group("count").formatIntOrNull()?.let { lastRewardCount = it }
            val dataSibling = chatComponent.siblings.firstOrNull() ?: return@matchMatcher
            dataSibling.getHoverLootPairs().forEach { (item, amount) ->
                loot.addOrPut(item, amount)
            }
        }

        phantomSpawnPattern.matchMatcher(message) {
            val mob = group("phantom")

            if (dropsJson.mobs.contains(mob) && config.compactGiftBonusDropsList.contains(DropCategory.MOBS))
                rareDrops.add("A wild §d$mob §fappeared!")
        }

        if (!openBonusGiftLoop)
            return

        val item = bonusGiftRewardPattern.matchMatcher(message) { group("item") } ?: return
        var itemInternalName = enchantedBookPattern.matchMatcher(item) {
            val book = group("book")
            val tier = group("tier").romanToDecimal()
            NeuInternalName.fromItemNameOrNull("$book $tier")
        } ?: NeuInternalName.fromItemNameOrNull(item) ?: return

        /**
         * this is a failsafe in the event of runes lacking sufficient NEU repo data to automagically
         * fetch their correct internal names, and thus translating their in-game names into internal
         * names literally
         */
        if (itemInternalName.startsWith(("◆_")))
            itemInternalName = itemInternalName.replace("◆_", "AXE_")

        loot.addOrPut(itemInternalName, 1)

        val bonusDropTypeList = config.compactGiftBonusDropsList
        val inCategoryList = dropsJsonCategories.any {
            it.category in bonusDropTypeList && it.items.contains(itemInternalName)
        }
        if (inCategoryList) rareDrops.add(item)
    }

    private fun SkyHanniChatEvent.Allow.tryBlock() {
        if (!config.compactGiftChats || !openLootLoop) return
        blockedReason = "TREE_GIFT"
    }

    private fun Component.getHoverLootPairs(): Set<Pair<NeuInternalName, Int>> = buildSet {
        val treeType = treeType ?: return this
        lastHover = hover
        val lootLines = hover?.formattedTextCompat()?.split("\n")?.takeIfNotEmpty() ?: return this
        ChatUtils.debug("found loot lines:\n${lootLines.joinToString("\n").replace("§", "&")}")
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

    @HandleEvent(IslandChangeEvent::class, onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onIslandChange() {
        firstUpdate()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetforagingtracker") {
            description = "Resets the Foraging Tracker."
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onItemChange(event: ItemInHandChangeEvent) {
        val isAxe = event.newItem.getItemStack().getItemCategoryOrNull() == ItemCategory.AXE
        if (isAxe != hasHeldAxe) {
            if (!isAxe) {
                lastAxeHeldTime = SimpleTimeMark.now()
            }
            hasHeldAxe = isAxe
        }
    }
}
