package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.accessories.AccessoryOverviewDisplayConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.inventory.AccessoriesUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.cakeBagPattern
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.personalXTorPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.SearchableScrollableRenderableTable
import kotlin.time.Duration.Companion.minutes

typealias AccStorage = ProfileSpecificStorage.StatsStorage.AccessoryStorage
private typealias DisplayTab = AccessoryOverviewDisplayConfig.AccessoryDisplayTab
private typealias MissingShowType = AccessoryOverviewDisplayConfig.MissingShowType
private typealias MissingSortType = AccessoryOverviewDisplayConfig.MissingSortType
private typealias IgnoreDupeItem = AccessoryOverviewDisplayConfig.IgnorableDuplicateItem

@SkyHanniModule
object AccessoryOverviewDisplay {

    private val config get() = SkyHanniMod.feature.inventory.stats.overviewDisplay
    private val storage get() = ProfileStorageData.profileSpecific?.stats
    private val inAccBag: Boolean get() = AccessoryApi.inAccessoryBag
    private val renderCache: MutableMap<DisplayTab, List<Renderable>> = enumMapOf()
    private var fullRenderCache: List<Renderable>? = null

    private const val NO_DATA_TEXT = """
        §c§lNo Accessory Data
        §7You have no accessory data stored.
        §7Once you start collecting accessories,
        §7this display will show you a summary.
    """

    private val noDataWarning by lazy {
        Renderable.verticalContainer(
            NO_DATA_TEXT.split("\n").map {
                Renderable.string(it)
            },
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
    }

    private var currentTab
        get() = config.selectedTab.get()
        set(value) {
            config.selectedTab.set(value)
        }

    private var lastBuiltAccHash: Int = 0

    private val tabSearchInputs = enumMapOf<DisplayTab, SearchTextInput>()
    private fun getSearchInputForTab(tab: DisplayTab) = tabSearchInputs.getOrPut(tab) { SearchTextInput() }

    private val tabScrollValues = enumMapOf<DisplayTab, ScrollValue>()
    private fun getScrollValueForTab(tab: DisplayTab) = tabScrollValues.getOrPut(tab) { ScrollValue() }

    private fun rebuildCaches(): List<Renderable> {
        val storage = storage?.accessoryStorage ?: return listOf()
        renderCache[DisplayTab.SUMMARY] = storage.buildSummaryTab()
        renderCache[DisplayTab.STATS] = storage.buildStatsTab()
        renderCache[DisplayTab.MISSING] = storage.buildMissingTab()
        renderCache[DisplayTab.DUPLICATES] = storage.buildDupesTab()
        return buildMainDisplay().also {
            fullRenderCache = it
        }
    }

    private val tipCache: TimeLimitedCache<Int, List<Renderable>> = TimeLimitedCache(5.minutes)

    @HandleEvent
    fun onAccessoriesUpdated(event: AccessoriesUpdatedEvent) {
        val newAccessories = event.accessories.takeIf { it.hashCode() != lastBuiltAccHash } ?: return
        rebuildCaches()
        lastBuiltAccHash = newAccessories.hashCode()
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.enabled || !inAccBag) return

        val fullRenderCache = fullRenderCache ?: run {
            rebuildCaches()
            fullRenderCache
        } ?: return

        config.position.renderRenderables(
            fullRenderCache,
            extraSpace = 5,
            posLabel = "Accessory Overview",
        )
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            config.selectedTab,
            config.missingTabShowType,
            config.missingTabSortType,
            config.maxHeight,
            config.ignoredDupeItems,
        ) { rebuildCaches() }
    }

    private fun getBaseTable(headerStrings: List<String> = listOf()) = SearchableScrollableRenderableTable(
        maxHeightGetter = { config.maxHeight.get() },
        scrollValueGetter = { getScrollValueForTab(currentTab) },
        searchInputGetter = { getSearchInputForTab(currentTab) },
        header = headerStrings.map { Renderable.string(it) }.toList(),
        showScrollableTipsInList = true,
    )

    private fun buildMainDisplay(): List<Renderable> = buildList {
        add(Renderable.string("§e§lAccessories Summary"))

        val toggleContainer = getToggleContainer()
        if (config.searchEnabled) add(
            Renderable.searchBox(
                toggleContainer,
                textInput = getSearchInputForTab(currentTab),
                searchPrefix = "Accessory Overview",
                onUpdateSize = { rebuildCaches() },
            )
        ) else add(toggleContainer)

        val mainContent = renderCache[currentTab] ?: listOf(noDataWarning)
        addAll(mainContent)
    }

    private fun getToggleContainer() = Renderable.verticalContainer(
        buildList {
            addTabToggle()
            addTabSpecificToggles()
        }
    )

    private fun MutableList<Renderable>.addTabToggle() =
        addRenderableButton<DisplayTab>(
            label = "Tab",
            current = currentTab,
            onChange = { currentTab = it },
        )

    private fun MutableList<Renderable>.addTabSpecificToggles(): Unit =
        when (config.selectedTab.get()) {
            DisplayTab.MISSING -> {
                this.addRenderableButton<MissingSortType>(
                    label = "Sort",
                    current = config.missingTabSortType.get(),
                    onChange = { config.missingTabSortType.set(it) },
                )
                this.addRenderableButton<MissingShowType>(
                    label = "Show",
                    current = config.missingTabShowType.get(),
                    onChange = { config.missingTabShowType.set(it) },
                )
            }

            else -> {}
        }

    private fun AccStorage.buildSummaryTab(): List<Renderable> = buildList {
        val table = getBaseTable(listOf("§7Rarity", "§7Count", "§7MP"))

        val rarities = LorenzRarity.entries.reversed().filter { rarity ->
            accessories.any { acc: Accessory ->
                acc.rarity == rarity
            }
        }

        rarities.forEach { rarity ->
            val properName = rarity.name.replace("_", " ")
            val nameRenderable = Renderable.string("${rarity.chatColorCode}$properName")

            val ofRarity = accessories.filter { it.rarity == rarity }
            val countRenderable = Renderable.string("§f${ofRarity.size}")

            val mpSum = ofRarity.sumOf { it.magicPower }
            val mpRenderable = Renderable.string("§b$mpSum")
            table.addRow(
                listOf(
                    nameRenderable,
                    countRenderable,
                    mpRenderable,
                ),
                properName,
            )
        }
        add(table.renderable)
    }

    private fun AccStorage.buildStatsTab(): List<Renderable> = buildList {
        val statsTable = getBaseTable(listOf("§7Stat", "§7Accessory Bonus"))

        val stats = accessories.flatMap { it.totalStats.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum() }

        stats.forEach { (stat, value) ->
            val statRenderable = Renderable.string(stat.iconWithName)
            val valueRenderable = Renderable.string("§f$value")
            statsTable.addRow(
                listOf(
                    statRenderable,
                    valueRenderable,
                ),
                stat.capitalizedName,
            )
        }
        add(statsTable.renderable)
    }

    private fun AccStorage.buildMissingTab(): List<Renderable> = buildList {
        val missingTable = getBaseTable(listOf("§7Accessory", "§7Cost", "§7Magical Power"))

        val showType = config.missingTabShowType.get()
        val missing = AccessoryApi.getMissing(this@buildMissingTab).filter { acc ->
            if (showType == MissingShowType.MAX_EACH_FAMILY) {
                acc.successor == null
            } else true
        }

        val sortType = config.missingTabSortType.get()
        val sortedList = when (sortType) {
            MissingSortType.RAW_MP -> missing.sortedByDescending { it.magicPower }
            MissingSortType.CHEAPEST -> missing.sortedBy { it.getUpgradeCost() }
            MissingSortType.BEST_RATIO -> missing.sortedByDescending {
                it.magicPower / it.getUpgradeCost()
            }

            else -> missing
        }

        sortedList.forEach { missingAcc ->
            missingAcc.buildMissingAccRow()?.let {
                missingTable.addRow(it, missingAcc.internalName.repoItemName)
            }
        }

        add(missingTable.renderable)
    }

    private fun Accessory.buildMissingAccTips(): List<Renderable> = buildList {
        add(Renderable.string("§7${internalName.repoItemName}"))
        val otherLines = buildList {
            // Price
            buildString {
                val doublePrice = internalName.getPriceOrNull()
                if (doublePrice != null && doublePrice > 0) append("§7Price: §6${doublePrice.toInt().addSeparators()}")
                else append("§7Price: §c§lNo price data!")
            }.takeIf { it.isNotEmpty() }?.let { add(Pair(Renderable.string(it), it)) }

            // Requirements
            usageSlayerRequirement?.matchLore?.takeIf { it.isNotEmpty() }?.let {
                add(Pair(Renderable.string(it), it))
            }
            craftSlayerRequirement?.matchLore?.takeIf { it.isNotEmpty() }?.let {
                add(Pair(Renderable.string(it), it))
            }
        }
        val longestLineCount = otherLines.maxOfOrNull { it.second.removeColor().length } ?: return this

        val dividerLine = "§7${"—".repeat(longestLineCount - 5)}"
        add(Renderable.string(dividerLine))
        otherLines.forEach { (renderable, _) ->
            add(renderable)
        }
        add(Renderable.string(dividerLine))
    }

    private fun Accessory.buildMissingAccRow(): List<Renderable>? = buildList {
        val itemStack = this@buildMissingAccRow.internalName.getItemStackOrNull() ?: return null
        val labelledIcon = Renderable.horizontalContainer(
            listOf(
                Renderable.itemStack(itemStack, scale = 0.5),
                Renderable.string(itemStack.displayName),
            ),
        )
        val clickable = Renderable.clickable(
            render = labelledIcon,
            onAnyClick = mapOf(
                LEFT_MOUSE to { ChatUtils.chat("$internalName clicked") }
            ), // todo
            tips = tipCache[this@buildMissingAccRow.hashCode()] ?: buildMissingAccTips().also {
                tipCache[this@buildMissingAccRow.hashCode()] = it
            },
            condition = { AccessoryApi.inAccessoryBag },
        )
        add(clickable)

        buildString {
            val price = getUpgradeCost().toInt()
            if (price > 0) append("§6${price.addSeparators()}")
            else append("§c§lNo price data!")
        }.takeIf { it.isNotEmpty() }?.let { add(Renderable.string(it)) }

        add(Renderable.string("§b$magicPower"))
    }

    private fun AccStorage.buildDupesTab(): List<Renderable> = buildList {
        val dupeTable = getBaseTable(listOf("§7Accessory", "§7Page"))

        val dupes = AccessoryApi.getDupes(this@buildDupesTab).filterDupes()
        dupes.forEach { dupe ->
            val dupeRenderable = dupe.buildMissingAccRow() ?: return@forEach
            dupeTable.addRow(dupeRenderable, dupe.internalName.repoItemName)
        }

        add(dupeTable.renderable)
    }

    private fun List<Accessory>.filterDupes(): List<Accessory> = this.filter {
        val filterTypes = config.ignoredDupeItems.get()
        val filterOutXTors = IgnoreDupeItem.PERSONAL_COMPACTORS_DELETORS in filterTypes
        val filterOutCakeBags = IgnoreDupeItem.CAKE_BAGS in filterTypes

        val passesXTorFilter = !filterOutXTors || !personalXTorPattern.matches(it.internalName.asString())
        val passesCakeBagFilter = !filterOutCakeBags || !cakeBagPattern.matches(it.internalName.toString())

        passesXTorFilter && passesCakeBagFilter
    }
}
