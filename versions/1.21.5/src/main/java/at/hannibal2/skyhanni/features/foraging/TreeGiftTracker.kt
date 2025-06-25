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
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import net.minecraft.text.Text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TreeGiftTracker {

    private val config get() = SkyHanniMod.feature.foraging.treeGiftTracker

    private val tracker = SkyHanniBucketedItemTracker(
        "Tree Gift Tracker",
        { TreeGiftTrackerLegacy.BucketData() },
        { it.foraging.treeGiftTracker },
        { drawDisplay(it) }
    )

    init {
        tracker.initRenderer({ config.position }) { isEnabled() }
    }

    private fun drawDisplay(bucketData: TreeGiftTrackerLegacy.BucketData): List<Searchable> = buildList {
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

    private fun isEnabled() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny() && heldItemEnabled()
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
    private var treeType: TreeGiftTrackerLegacy.TreeType? = null
    private var lastTreeGiftAt: SimpleTimeMark = SimpleTimeMark.farPast()
    private val loot = mutableMapOf<NeuInternalName, Int>()

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onChat(event: SkyHanniChatEvent) {
        event.tryReadLoot()
        event.tryBlock()
    }

    private fun SkyHanniChatEvent.tryReadLoot() {
        TreeGiftTrackerLegacy.openCloseRewardPattern.matchMatcher(message) {
            openLootLoop = !openLootLoop
            if (openLootLoop) {
                openBonusGiftLoop = false
                lastTreeGiftAt = SimpleTimeMark.now()
            } else {
                sendTreeGiftStats()
            }
            if (config.hideChats) blockedReason = "TREE_GIFT"
        }
        if (!openLootLoop) return

        TreeGiftTrackerLegacy.bonusGiftSeparatorPattern.matchMatcher(message) {
            openBonusGiftLoop = true
            return
        }

        TreeGiftTrackerLegacy.percentageContributedPattern.matchMatcher(message) {
            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            val percentColor = group("percentColor")
            lastPercentString = "$percentColor$percentage%"
            val type = group("type")
            treeType = TreeGiftTrackerLegacy.TreeType.byNameOrNull(type)
            val treeType = treeType ?: return@matchMatcher
            tracker.modify {
                it.treesCut.addOrPut(treeType, 1)
                it.wholeTreesCut.addOrPut(treeType, percentage / 100.0)
            }
        }

        TreeGiftTrackerLegacy.rewardsGainedPattern.matchMatcher(message) {
            group("count").formatIntOrNull()?.let { lastRewardCount = it }
            val dataSibling = chatComponent.siblings.firstOrNull() ?: return@matchMatcher
            dataSibling.getHoverLootPairs().forEach { (item, amount) ->
                loot.addOrPut(item, amount)
            }
        }

        if (!openBonusGiftLoop) return
        TreeGiftTrackerLegacy.bonusGiftRewardPattern.matchMatcher(message) {
            val item = group("item")
            val itemInternalName = TreeGiftTrackerLegacy.enchantedBookPattern.matchMatcher(item) {
                val book = group("book")
                val tier = group("tier").romanToDecimal()
                NeuInternalName.fromItemNameOrNull("$book $tier")
            } ?: NeuInternalName.fromItemNameOrNull(item) ?: return@matchMatcher
            loot.addOrPut(itemInternalName, 1)

            val percentage = group("percentage").formatDoubleOrNull() ?: return@matchMatcher
            if (percentage <= 1.0) rareDrops.add(item)
        }
    }

    private fun SkyHanniChatEvent.tryBlock() {
        if (!config.hideChats || !openLootLoop) return
        blockedReason = "TREE_GIFT"
    }

    private fun Text.getHoverLootPairs(): Set<Pair<NeuInternalName, Int>> = buildSet {
        val treeType = treeType ?: return this
        lastHover = hover
        val joinedLines = hover?.formattedTextCompat() + hover?.siblings?.joinToString("") { it.formattedTextCompat() }
        joinedLines.split("\n").forEach { line ->
            val (item, amountString) = TreeGiftTrackerLegacy.hoverRewardPattern.matchMatcher(line) {
                group("item") to group("amount")
            } ?: return@forEach
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
        val message = "§9$lastTreeType Tree Gift. §7You helped cut $lastPercentString §7and gained §e$lastRewardCount rewards§a!"
        val component = message.asComponent()
        component.hover = lastHover
        ChatUtils.chat(component)
        rareDrops.forEach { drop ->
            ChatUtils.chat("§f - $drop", prefix = false)
        }
        rareDrops.clear()
        lastHover = null
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
