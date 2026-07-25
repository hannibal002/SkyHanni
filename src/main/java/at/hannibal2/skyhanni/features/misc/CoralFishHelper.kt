package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ItemBuyApi.buy
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import at.hannibal2.skyhanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchableScrollable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CoralFishHelper {

    private val config get() = SkyHanniMod.feature.misc

    private const val OVERVIEW_FISH_SLOT = 4

    private val patternGroup = RepoPattern.group("misc.coral.fish")

    /**
     * REGEX-TEST: Nope the Fish
     * REGEX-TEST: Cluck the Fish
     * REGEX-TEST: Herring the Fish
     */
    private val coralFishNamePattern by patternGroup.pattern(
        "name.colorless",
        "\\w+ the Fish",
    )

    /**
     * REGEX-TEST: Fish Shown: 1/31
     * REGEX-TEST: Fish Shown: 15/31
     */
    private val coralFishFoundPattern by patternGroup.pattern(
        "shown",
        "Fish Shown: (?<found>\\d+)/(?<total>\\d+)",
    )

    /**
     * REGEX-TEST: (1/31) Fish Family
     * REGEX-TEST: (15/31) Fish Family
     */
    private val coralMenuPattern by patternGroup.pattern(
        "inventory",
        "\\(\\d+/\\d+\\) Fish Family",
    )

    init {
        InventoryDetector(
            onOpenInventory = { DelayedRun.runNextTick { checkInventoryItems() } },
        ) { coralMenuPattern }
    }

    private var display = emptyList<Renderable>()
    private val textInput = SearchTextInput()

    private fun checkInventoryItems() {
        val items = InventoryUtils.getItemsInOpenChest().map { it.item }

        val overviewItem = items[OVERVIEW_FISH_SLOT]
        val overviewItemLore = overviewItem.getCleanLore()

        val (amountFound, totalAmount) = coralFishFoundPattern.firstMatcher(overviewItemLore) {
            group("found").toInt() to group("total").toInt()
        } ?: return

        val neededFish = mutableListOf<String>()

        for (item in items) {
            val itemName = item.cleanName
            if (!coralFishNamePattern.matches(itemName)) continue

            if (!item.isDye(DyeCompat.GRAY)) continue
            neededFish.add(itemName)
        }
        constructDisplay(neededFish, amountFound, totalAmount)
    }

    private fun constructDisplay(fishList: List<String>, found: Int, total: Int) {
        val items = fishList.mapNotNull { NeuInternalName.fromItemNameOrNull(it) }
        val displayLines = items.map { createRenderableLine(it) }

        val sorted = displayLines.sortedBy { it.price }

        display = buildList {
            addString("§dCoral Fish Helper")
            if (found == total) {
                addString("§a§lYou have found all the fish!")
            } else {
                addString("§7You've found §a$found§7 out of §a$total§7 fish!")
                addString("§7Click on a fish to search for it on the ah!")
                add(sorted.map { it.searchable }.buildSearchableScrollable(height = 225, textInput, velocity = 25.0))
            }
        }
    }

    private fun createRenderableLine(internalName: NeuInternalName): CoralFishHelperLine {
        val stack = Renderable.item(internalName.getItemStack())
        val price = internalName.getPrice()
        val priceString = if (price > 0.0) "§6${price.shortFormat()} coins" else "§cNo Price Found"
        val itemName = internalName.repoItemName

        val tooltip = buildList {
            add(itemName)
            add("§7Lowest bin: $priceString")
            add("")
            add("§eClick to open on the ah!")
        }

        val clickable = Renderable.clickable(
            " §7- §a${itemName.removeColor()} $priceString",
            tips = tooltip,
            onLeftClick = {
                internalName.buy(1)
            },
        )

        val container = Renderable.horizontal(stack, clickable).toSearchable()

        return CoralFishHelperLine(price, container)
    }

    private data class CoralFishHelperLine(
        val price: Double,
        val searchable: Searchable,
    )

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onChestGuiRender() {
        if (!config.coralFishHelper) return
        if (display.isEmpty()) return
        config.coralFishHelperPosition.renderRenderables(display, posLabel = "Coral Fish Helper")
    }

    @HandleEvent
    fun onInventoryClose() {
        display = emptyList()
    }
}
