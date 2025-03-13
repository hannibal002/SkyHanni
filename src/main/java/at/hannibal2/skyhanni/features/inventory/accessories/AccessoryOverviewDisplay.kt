package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.accessories.AccessoryOverviewDisplayConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.inventory.AccessoriesUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.SearchableRenderableTable

typealias AccStorage = ProfileSpecificStorage.StatsStorage.AccessoryStorage
private typealias DisplayTab = AccessoryOverviewDisplayConfig.AccessoryDisplayTab
private typealias MissingShowType = AccessoryOverviewDisplayConfig.MissingShowType
private typealias MissingSortType = AccessoryOverviewDisplayConfig.MissingSortType

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

    private var missingSortType
        get() = config.missingTabSortType.get()
        set(value) {
            config.missingTabSortType.set(value)
        }

    private var missingShowType
        get() = config.missingTabShowType.get()
        set(value) {
            config.missingTabShowType.set(value)
        }

    private var lastBuiltAccHash: Int = 0

    private val tabSearchInputs = enumMapOf<DisplayTab, SearchTextInput>()
    private fun getSearchInputForTab(tab: DisplayTab) = tabSearchInputs.getOrPut(tab) { SearchTextInput() }

    private fun rebuildCaches(): List<Renderable> {
        val storage = storage?.accessoryStorage ?: return listOf()
        renderCache[DisplayTab.SUMMARY] = storage.buildSummaryTab()
        renderCache[DisplayTab.STATS] = storage.buildStatsTab()
        renderCache[DisplayTab.MISSING] = storage.buildMissingTab()
        return buildMainDisplay().also {
            fullRenderCache = it
        }
    }

    @HandleEvent
    fun onAccessoriesUpdated(event: AccessoriesUpdatedEvent) {
        val newAccessories = event.accessories.takeIf { it.hashCode() != lastBuiltAccHash } ?: return
        rebuildCaches()
        lastBuiltAccHash = newAccessories.hashCode()
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.enabled || !inAccBag) return

        val storage = storage?.accessoryStorage ?: return
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
        ) { rebuildCaches() }
    }

    private fun buildMainDisplay(): List<Renderable> = buildList {
        add(Renderable.string("§e§lAccessories Summary"))
        addTabToggle()
        addTabSpecificToggles()
        val mainContent = renderCache[currentTab] ?: listOf(noDataWarning)
        addAll(mainContent)
    }

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
                    current = missingSortType,
                    onChange = { missingSortType = it },
                )
                this.addRenderableButton<MissingShowType>(
                    label = "Show",
                    current = missingShowType,
                    onChange = { missingShowType = it },
                )
            }

            else -> {}
        }

    private fun AccStorage.buildSummaryTab(): List<Renderable> = buildList {
        add(Renderable.underlined(Renderable.string("§eCount by Rarity")))
        val table = SearchableRenderableTable { getSearchInputForTab(currentTab) }.apply {
            val headers = listOf("§7Rarity", "§7Count", "§7MP")
            addRow(headers.map { Renderable.string(it) }.toList(), "")
        }
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
        add(Renderable.underlined(Renderable.string("§eAccessory Stats")))
        val statsTable = SearchableRenderableTable { getSearchInputForTab(currentTab) }.apply {
            val headers = listOf("§7Stat", "§7Accessory Bonus")
            addRow(headers.map { Renderable.string(it) }.toList(), "")
        }

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
        add(Renderable.underlined(Renderable.string("§eMissing Accessories")))
        val missingTable = SearchableRenderableTable { getSearchInputForTab(currentTab) }.apply {
            val headers = listOf("§7Accessory", "§7Cost", "§7Magical Power")
            addRow(headers.map { Renderable.string(it) }.toList(), "")
        }

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
            missingTable.addRow(missingAcc.buildRow(), missingAcc.internalName.itemName)
        }

        add(missingTable.renderable)
    }

    private fun Accessory.buildRow() = buildList {
        val itemStack = this@buildRow.internalName.getItemStackOrNull() ?: run {
            ChatUtils.chat("Item stack for $internalName is null")
            return@buildList
        }
        val itemStackRender = Renderable.itemStack(
            itemStack,
            scale = 0.5,
        )
        val displayName = itemStack.displayName
        add(
            Renderable.horizontalContainer(
                listOf(
                    itemStackRender,
                    Renderable.string(displayName),
                ),
            ),
        )
        add(Renderable.string("§6${getUpgradeCost().toInt().addSeparators()}"))
        add(Renderable.string("§b$magicPower"))
    }
}
