package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.inventory.accessories.AccessoryOverviewDisplayConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.inventory.AccessoriesUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryOverviewDisplay.constructCaches
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.SearchableRenderableTable

private typealias AccStorage = ProfileSpecificStorage.StatsStorage.AccessoryStorage
private typealias DisplayTab = AccessoryOverviewDisplayConfig.AccessoryDisplayTab

@SkyHanniModule
object AccessoryOverviewDisplay {

    private val config get() = SkyHanniMod.feature.inventory.stats
    private val storage get() = ProfileStorageData.profileSpecific?.stats
    private val inAccBag: Boolean get() = AccessoryApi.inAccessoryBag
    private val renderCache: MutableMap<DisplayTab, List<Renderable>> = enumMapOf()
    private var currentTab
        get() = config.overviewDisplay.selectedTab.get()
        set(value) { config.overviewDisplay.selectedTab.set(value) }

    private var lastBuiltAccHash: Int = 0

    private val tabSearchInputs = enumMapOf<DisplayTab, SearchTextInput>()
    private fun getSearchInputForTab(tab: DisplayTab) = tabSearchInputs.getOrPut(tab) { SearchTextInput() }

    // <editor-fold desc="Event Handlers">
    @HandleEvent
    fun onAccessoriesUpdated(event: AccessoriesUpdatedEvent) {
        val newAccessories = event.accessories.takeIf { it.hashCode() != lastBuiltAccHash } ?: return
        lastBuiltAccHash = newAccessories.hashCode()
        newAccessories.constructCaches()
    }

    private const val DEBUG = true
    private val flatCacheSet get() = renderCache.values.flatten()

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.overviewDisplay.enabled || !inAccBag) return
        val storage = storage?.accessoryStorage ?: return
        if (renderCache.isEmpty()) storage.constructCaches()

        config.overviewDisplay.position.renderRenderables(
            storage.buildMainDisplay(),
            extraSpace = 5,
            posLabel = "Accessory Overview",
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shrebuildaccscache") {
            description = "Forcefully rebuild the accessory cache."
            category = CommandCategory.DEVELOPER_DEBUG
            callback { storage?.accessoryStorage?.constructCaches() }
        }

    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.overviewDisplay.selectedTab.onToggle { renderCache.clear() }
    }
    // </editor-fold>

    // <editor-fold desc="Main Builder">
    private fun AccStorage.constructCaches() {
        renderCache.clear()
        renderCache[DisplayTab.SUMMARY] = buildSummaryTab()
    }

    private fun AccStorage.buildMainDisplay(): List<Renderable> = buildList {
        constructCaches()
        add(Renderable.string("§e§lAccessories Summary"))
        addTabToggle()
        addTabSpecificToggles()
        addEmptyLine()
        val mainContent = renderCache[currentTab] ?: listOf(getNoDataWarning())
        addAll(mainContent)
    }
    // </editor-fold>

    // <editor-fold desc="Helpers">
    private fun MutableList<Renderable>.addEmptyLine() = add(Renderable.string(""))

    private fun MutableList<Renderable>.addTabToggle() =
        addRenderableButton<DisplayTab>(
            label = "Tab",
            current = currentTab,
            onChange = { currentTab = it },
        )

    private fun MutableList<Renderable>.addTabSpecificToggles() =
        when (config.overviewDisplay.selectedTab.get()) {
            DisplayTab.STATS -> " todo"
            DisplayTab.MISSING -> {
                // Todo: Show all tiers/show only max tier
                " todo "
            }
            DisplayTab.DUPLICATES -> {
                // Todo: Separate toggles (combined list with selector?) for counting compactors/cake bags as dupes
                "todo "
            }
            else -> "" // No specific toggles
        }

    private const val NO_DATA_TEXT = """
        §c§lNo Accessory Data
        §7You have no accessories in your accessory bag.
        §7Accessories are items that can be equipped in the accessory bag.
        §7They provide various stats and abilities.
        §7You can obtain accessories from various sources, such as dungeons, slayers, and events.
    """

    private fun getNoDataWarning(): Renderable = Renderable.verticalContainer(
        buildList {
            NO_DATA_TEXT.split("\n").forEach { line ->
                add(Renderable.string(line))
            }
        },
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
    )
    // </editor-fold>

    // <editor-fold desc="Summary Tab">
    private fun AccStorage.buildSummaryTab(): MutableList<Renderable> = buildList {
        addAll(buildRaritySummaryRows())
        add(getNoDataWarning())
    }.toMutableList()

    private fun AccStorage.buildRaritySummaryRows(): List<Renderable> = buildList {
        add(Renderable.underlined(Renderable.string("§eCount by Rarity")))
        val table = SearchableRenderableTable { getSearchInputForTab(currentTab) }.apply {
            // Header for the table
            val headers = listOf("§7Rarity", "§7Count", "§7MP")
            addRow(headers.map { Renderable.string(it) }.toList(), "")
        }
        val rarities = LorenzRarity.entries.reversed().filter { rarity ->
            accessories.any { acc ->
                acc.rarity == rarity
            }
        }

        ChatUtils.chat("accessories.size: ${accessories.size}")
        ChatUtils.chat("rarities: " + rarities.joinToString(", "))

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
                properName
            )
        }
        add(table.renderable)
    }
    // </editor-fold>
}
