package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
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
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import net.minecraft.text.Text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ForagingTracker {

    private val config get() = SkyHanniMod.feature.foraging.tracker

    private val tracker = SkyHanniBucketedItemTracker(
        "Foraging Tracker",
        { ForagingTrackerLegacy.BucketData() },
        { it.foraging.trackerData },
        { drawDisplay(it) },
    )

    init {
        tracker.initRenderer({ config.position }) { isInIsland() && heldItemEnabled() && config.enabled }
    }

    private fun heldItemEnabled() = !config.onlyHoldingAxe || isHoldingAxe()
    private fun isHoldingAxe() = InventoryUtils.getItemInHand()?.getItemCategoryOrNull() == ItemCategory.AXE

    private fun drawDisplay(bucketData: ForagingTrackerLegacy.BucketData): List<Searchable> = buildList {
        addSearchString("§a§lForaging Tracker")
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

        val bucketFormat = bucketData.selectedBucket?.let { "$it " }.orEmpty()
        val baseFormat = "${bucketFormat}Trees Felled:"

        val wholeTreesFelled = bucketData.getWholeTreeCount()
        if (config.showWholeTrees && wholeTreesFelled > 0.0) {
            val preambleFormat = "Whole $baseFormat"
            val wholeRenderable = Renderable.hoverTips(
                StringRenderable("§e$preambleFormat ${wholeTreesFelled.addSeparators()}"),
                tips = bucketData.wholeTreesCut.mapNotNull { (treeType, count) ->
                    if (count <= 0.0) return@mapNotNull null
                    "§7Whole $treeType Trees cut: §a${count.addSeparators()}"
                },
            ).toSearchable("whole trees felled")
            add(wholeRenderable)
        }

        val totalRenderable = Renderable.hoverTips(
            StringRenderable("§e$baseFormat ${treesContributedTo.addSeparators()}"),
            tips = bucketData.treesCut.mapNotNull { (treeType, count) ->
                if (count <= 0) return@mapNotNull null
                "$treeType Tree contributions: §a${count.addSeparators()}"
            },
        ).toSearchable("trees felled")
        add(totalRenderable)


        add(tracker.addTotalProfit(profit, treesContributedTo, "gift"))
        tracker.addPriceFromButton(this)
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!isInIsland() || event.source != ItemAddManager.Source.COMMAND) return
        with(tracker) {
            event.addItemFromEvent()
        }
    }

    private val rangedItems: MutableSet<NeuInternalName> = mutableSetOf()

    @HandleEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isInIsland()) return
        event.readTreeGifts()
        event.addLogs()
    }

    private fun SackChangeEvent.readTreeGifts() {
        val treeType = treeType ?: return
        if (lastTreeGiftAt.passedSince() >= 30.seconds) return
        sackChanges.filter {
            it.delta > 0 && it.internalName in rangedItems
        }.forEach {
            tracker.addItem(
                treeType,
                it.internalName,
                it.delta,
                command = false,
            )
        }
    }

    private data class LogSackChange(
        val treeType: ForagingTrackerLegacy.TreeType,
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
        if (delta > 0)
            tracker.addItem(treeType, baseLog, delta, command = false)

        val enchantedLog = treeType.getEnchantedLog()
        if (deltaEnchanted > 0)
            tracker.addItem(treeType, enchantedLog, deltaEnchanted, command = false)
    }

    private fun SackChangeEvent.extractLogs(): List<LogSackChange> = sackChanges.asSequence()
        .filter { it.delta > 0 }.mapNotNull { change ->
            ForagingTrackerLegacy.logInternalNamePattern.matchMatcher(change.internalName.asString()) {
                val type = ForagingTrackerLegacy.TreeType.byNameOrNull(group("treeType"))
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
    private var treeType: ForagingTrackerLegacy.TreeType? = null
    private var lastTreeGiftAt: SimpleTimeMark = SimpleTimeMark.farPast()
    private val loot = mutableMapOf<NeuInternalName, Int>()

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isInIsland()) return
        event.tryReadLoot()
        event.tryBlock()
    }

    private val STRETCHING_STICKS = "STRETCHING_STICKS".toInternalName()
    private var currentStretchingSticks = 0

    @HandleEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!isInIsland()) return
        val treeType = treeType ?: return

        val stretchingSticksNow = InventoryUtils.getItemsInOwnInventory().filter {
            it.getInternalNameOrNull() == STRETCHING_STICKS
        }.sumOf { it.count }

        val change = stretchingSticksNow - currentStretchingSticks
        currentStretchingSticks = stretchingSticksNow
        if (change <= 0) return
        tracker.addItem(treeType, STRETCHING_STICKS, change, command = false)
    }

    private fun SkyHanniChatEvent.tryReadLoot() {
        ForagingTrackerLegacy.openCloseRewardPattern.matchMatcher(message) {
            openLootLoop = !openLootLoop
            if (openLootLoop) {
                openBonusGiftLoop = false
                lastTreeGiftAt = SimpleTimeMark.now()
            } else {
                sendTreeGiftStats()
                val treeType = treeType ?: ForagingTrackerLegacy.TreeType.FIG
                loot.forEach { (item, count) ->
                    tracker.addItem(treeType, item, count, command = false)
                }
                loot.clear()
            }
            if (config.compactGiftChats) blockedReason = "TREE_GIFT"
        }
        if (!openLootLoop) return

        ForagingTrackerLegacy.bonusGiftSeparatorPattern.matchMatcher(message) {
            openBonusGiftLoop = true
            return
        }

        ForagingTrackerLegacy.percentageContributedPattern.matchMatcher(message) {
            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            val percentColor = group("percentColor")
            lastPercentString = "$percentColor$percentage%"
            val type = group("type")
            treeType = ForagingTrackerLegacy.TreeType.byNameOrNull(type)
            val treeType = treeType ?: return@matchMatcher
            tracker.modify {
                it.treesCut.addOrPut(treeType, 1)
                it.wholeTreesCut.addOrPut(treeType, percentage / 100.0)
            }
        }

        ForagingTrackerLegacy.rewardsGainedPattern.matchMatcher(message) {
            group("count").formatIntOrNull()?.let { lastRewardCount = it }
            val dataSibling = chatComponent.siblings.firstOrNull() ?: return@matchMatcher
            dataSibling.getHoverLootPairs().forEach { (item, amount) ->
                loot.addOrPut(item, amount)
            }
        }

        if (!openBonusGiftLoop) return
        ForagingTrackerLegacy.bonusGiftRewardPattern.matchMatcher(message) {
            val item = group("item")
            val itemInternalName = ForagingTrackerLegacy.enchantedBookPattern.matchMatcher(item) {
                val book = group("book")
                val tier = group("tier").romanToDecimal()
                NeuInternalName.fromItemNameOrNull("$book $tier")
            } ?: NeuInternalName.fromItemNameOrNull(item) ?: return@matchMatcher

            // Stretching Sticks handled separately due to stack size not being given in message
            if (itemInternalName == STRETCHING_STICKS) return@matchMatcher

            loot.addOrPut(itemInternalName, 1)

            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            if (percentage <= 1.0) rareDrops.add(item)
        }
    }

    private fun SkyHanniChatEvent.tryBlock() {
        if (!config.compactGiftChats || !openLootLoop) return
        blockedReason = "TREE_GIFT"
    }

    private fun Text.getHoverLootPairs(): Set<Pair<NeuInternalName, Int>> = buildSet {
        val treeType = treeType ?: return this
        lastHover = hover
        val lootLines = hover?.formattedTextCompat()?.split("\n")?.takeIfNotEmpty() ?: return this
        ChatUtils.debug("found loot lines:\n${lootLines.joinToString("\n").replace("§", "&")}")
        lootLines.forEach { line ->
            val (item, amountString) = ForagingTrackerLegacy.hoverRewardPattern.matchMatcher(line) {
                val amountString = if (groupOrNull("percentage") != null) "1" else group("amount")
                group("item") to amountString
            } ?: return@forEach
            ChatUtils.debug("found hover loot: $item x$amountString")
            if (amountString.contains("-")) {
                NeuInternalName.fromItemNameOrNull(item)?.let {
                    // Skip ranges like "0-2" (for now), handle from sack changes
                    rangedItems.add(it)
                }
                return@forEach
            }
            val amount = amountString.formatIntOrNull() ?: return@forEach
            when (item) {
                "HOTF Experience" -> tracker.modify {
                    it.hotfExperience.addOrPut(treeType, amount.toLong())
                }

                "Foraging Experience" -> tracker.modify {
                    it.foragingExperience.addOrPut(treeType, amount.toLong())
                }

                "Forest Whispers" -> tracker.modify {
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
    private var lastHover: Text? = null

    private fun sendTreeGiftStats() {
        val lastTreeType = treeType ?: return
        if (config.compactGiftChats) {
            val message = "§9$lastTreeType Tree Gift. §7You helped cut $lastPercentString §7and gained §e$lastRewardCount rewards§a!"
            val component = message.asComponent()
            component.hover = lastHover
            ChatUtils.chat(component)
            rareDrops.forEach { drop ->
                ChatUtils.chat("§f - $drop", prefix = false)
            }
        }
        rareDrops.clear()
        lastHover = null
    }

    @HandleEvent(IslandChangeEvent::class)
    fun onIslandChange() {
        if (!isInIsland()) return
        tracker.firstUpdate()
    }

    private fun isInIsland() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetforagingtracker") {
            description = "Resets the Foraging Tracker."
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }

}
