package at.hannibal2.skyhanni.features.inventory.searchablestorage

import at.hannibal2.skyhanni.api.StorageApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.minecraftButton
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.searchMode
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.sortMode
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.storagePattern
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard
import java.awt.Color

class SearchableStorageGui(private var search: String = "") : SkyhanniBaseScreen() {

    private var displayedItems = mapOf<NeuInternalName, ItemRenderData>()
    private var allItems = mapOf<NeuInternalName, ItemRenderData>()
    private var selectedPage = 0
    private var pageRows = 0
    private var pageColumns = 0
    private var selectedItem: Pair<NeuInternalName, ItemRenderData>? = null
    private var displayedStorages = listOf<StorageRenderData>()

    private var headerHeight = 0
    private var guiLeft = 0
    private var guiTop = 0
    private var display: Renderable? = null
    private var searchModeRenderable: Pair<Renderable, List<Renderable>>? = null
    private var sortModeRenderable: Pair<Renderable, List<Renderable>>? = null
    private var footerRenderable: Pair<Renderable, List<Renderable>>? = null

    data class ItemRenderData(
        val data: ItemData,
        val renderable: Renderable,
        val position: Pair<Int, Int>,
    )

    data class ItemData(
        var amount: Int,
        val inventoryContainers: MutableList<SkyHanniInventoryContainer>,
    )

    data class StorageRenderData(
        val inventoryContainer: SkyHanniInventoryContainer,
        val visual: StorageVisualData,
        val position: Pair<Int, Int>,
    )

    data class StorageVisualData(
        val renderable: Renderable,
        val highlightSlots: List<Int>,
    )

    enum class SearchMode(val displayName: String, val matches: (ItemStack, String) -> Boolean) {
        NAME("NAME", { stack, search -> stack.displayName.removeColor().contains(search, ignoreCase = true) }),
        LORE("LORE", { stack, search -> stack.getLore().any { it.removeColor().contains(search, ignoreCase = true) } }),
        NAME_LORE("NAME + LORE", { stack, search -> NAME.matches(stack, search) || LORE.matches(stack, search) })
        ;

        override fun toString() = if (searchMode == this) " §f${this.displayName} " else " §7${this.displayName} "
    }

    enum class SortMode(
        val displayName: String,
        private val comparator: Comparator<Map.Entry<NeuInternalName, ItemData>>,
    ) {
        NAME_ASC("NAME ⬆", compareBy { it.key.getItemStack().displayName.lowercase() }),
        NAME_DESC("NAME ⬇", compareByDescending { it.key.getItemStack().displayName.lowercase() }),
        PRICE_ASC("PRICE ⬆", compareBy { it.key.getPrice() * it.value.amount }),
        PRICE_DESC("PRICE ⬇", compareByDescending { it.key.getPrice() * it.value.amount });

        fun sort(input: Map<NeuInternalName, ItemData>): Map<NeuInternalName, ItemData> =
            input.entries.sortedWith(comparator).associate { it.toPair() }

        override fun toString(): String =
            if (sortMode == this) " §f$displayName " else " §7$displayName "
    }

    override fun guiClosed() {
        OtherInventoryData.close("Searchable Storage GUI")
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)

        display = Renderable.drawInsideRoundedRect(buildContent(), Color.decode("#202020"))

        display?.let { display ->
            guiLeft = (width - display.width) / 2
            guiTop = (height - display.height) / 2

            GlStateManager.disableLighting()
            DrawContextUtils.pushPop {
                DrawContextUtils.translate(guiLeft.toFloat(), guiTop.toFloat(), 0f)
                display.render(guiLeft, guiTop)
            }
        }
    }

    private fun buildContent(): Renderable {
        val header = buildHeader()

        val content = when {
            search.isBlank() && displayedItems.isEmpty() -> buildSearch(searchStorage())

            selectedItem != null -> {
                val internalName = selectedItem?.first ?: return StringRenderable("§cError while rendering containers :(")
                buildSearch(displayStorages(internalName))
            }

            search.isNotBlank() && displayedItems.isEmpty() -> buildSearch(searchStorage())

            else -> buildSearch(createItemDisplay(displayedItems.mapValues { it.value.data }, update = false))
        }

        val footer = buildFooter()

        val placeholder = Renderable.placeholder(1)

        return Renderable.vertical(listOf(header, placeholder, content, placeholder, footer))
    }

    private fun buildHeader(): Renderable {
        val searchField = StringRenderable(
            "§eSearch: §7$search",
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        )

        val searchMode = buildModeField("§eSearch by:", SearchMode.entries)
        searchModeRenderable = searchMode.first to searchMode.second

        val sortMode = buildModeField("§eSort by:", SortMode.entries)
        sortModeRenderable = sortMode.first to sortMode.second

        return Renderable.vertical(listOf(searchField, searchMode.first, sortMode.first)).also {
            headerHeight = it.height + 10
        }
    }

    private fun buildModeField(
        label: String,
        entries: List<Any>,
    ): Pair<Renderable, List<StringRenderable>> {
        val separator = StringRenderable("§e|")
        val renderables = listOf(StringRenderable("§e$label")) +
            entries.map { StringRenderable(it.toString()) }
                .flatMapIndexed { i, r -> if (i < entries.size - 1) listOf(r, separator) else listOf(r) }

        val field = Renderable.horizontal(
            renderables,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        )
        return field to renderables
    }

    private fun buildSearch(results: List<List<Renderable>>): Renderable {
        return if (results.isEmpty()) {
            StringRenderable(
                "§cNothing found :( Did you open all your storages?",
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        } else {
            Renderable.table(results)
        }
    }

    private fun buildFooter(): Renderable {
        val per = pageRows * pageColumns
        val maxPages = when {
            displayedItems.isEmpty() -> 0
            else -> (allItems.size + (per - 1)) / per
        }

        val buttonPrevious = minecraftButton("<")
        val buttonNext = minecraftButton(">")

        val page = StringRenderable("§f$selectedPage§7/§f$maxPages")

        val footer =
            Renderable.horizontal(buttonPrevious, page, buttonNext, spacing = 15, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)

        footerRenderable = footer to listOf(buttonPrevious, page, buttonNext)

        return footer
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (displayedStorages.isNotEmpty()) {
                    displayedStorages = listOf()
                    selectedItem = null
                } else if (search.isNotEmpty()) {
                    search = search.dropLast(1)
                    searchStorage()
                }
            }

            else -> if (typedChar != null && typedChar.isLetterOrDigit() || typedChar in listOf(' ', '_', '-')) {
                search += typedChar
                searchStorage()
            }
        }
    }

    override fun onHandleMouseInput() {
        val (relativeMouseX, relativeMouseY) = getRelativeMousePos()

        for ((item, data) in displayedItems) {
            if (displayedStorages.isNotEmpty()) break
            val isHovered = GuiRenderUtils.isPointInRect(
                relativeMouseX,
                relativeMouseY,
                data.position.first,
                data.position.second,
                data.renderable.width,
                data.renderable.height,
            )

            if (!isHovered) continue
            
            item.getItemStack().getLore()
//             renderToolTip(item.getItemStack(), relativeMouseX, relativeMouseY)
        }
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        val (relativeMouseX, relativeMouseY) = getRelativeMousePos()

        searchModeRenderable.handleClick(relativeMouseX, relativeMouseY, { 0 }, { it.width }, { it.height * 1 }, { it % 2 != 0 }) { index ->
            searchMode = SearchMode.entries.getOrNull((index - 1) / 2) ?: searchMode
        }

        sortModeRenderable.handleClick(relativeMouseX, relativeMouseY, { 0 }, { it.width }, { it.height * 2 }, { it % 2 != 0 }) { index ->
            sortMode = SortMode.entries.getOrNull((index - 1) / 2) ?: sortMode
        }

        val before = selectedPage
        footerRenderable.handleClick(
            relativeMouseX, relativeMouseY,
            { ((display?.width ?: 0) - it.width) / 2 },
            { it.width + 15 },
            { (display?.height ?: 0) - it.height },
            { it != 1 },
        ) { index ->
            val per = pageRows * pageColumns
            val pages = (allItems.size + (per - 1)) / per
            when {
                index == 0 && selectedPage == 1 -> selectedPage = pages
                index == 0 && selectedPage != 1 -> selectedPage--
                index == 2 && selectedPage == pages -> selectedPage = 1
                index == 2 && selectedPage != pages -> selectedPage++
            }
            val start = 0 + (per * (selectedPage - 1))
            val end = per * selectedPage

            println(displayedItems)
            displayedItems =
                allItems.entries.toList().subList(start, if (allItems.size < end) allItems.size else end).associate { it.toPair() }
            println(displayedItems)
        }
        if (before != selectedPage) return

        for ((item, data) in displayedItems) {
            if (displayedStorages.isNotEmpty()) break
            val isHovered = GuiRenderUtils.isPointInRect(
                relativeMouseX,
                relativeMouseY,
                data.position.first,
                data.position.second,
                data.renderable.width,
                data.renderable.height,
            )

            if (!isHovered) continue

            if (mouseButton == 0) {
                selectedItem = item to data
            }
        }

        for (storage in displayedStorages) {
            val isHovered = GuiRenderUtils.isPointInRect(
                relativeMouseX,
                relativeMouseY,
                storage.position.first,
                storage.position.second,
                storage.visual.renderable.width,
                storage.visual.renderable.height,
            )

            if (!isHovered) continue

            if (mouseButton == 0) {
                storagePattern.matchMatcher(storage.inventoryContainer.displayName) {
                    val type = group("type")
                    val page = group("page").toInt()

                    when (type) {
                        "Backpack" -> {
                            HypixelCommands.backPack(page)
                            SearchableStorage.inventoryName = "Backpack§r (Slot #$page)"
                        }

                        "Ender Chest" -> {
                            HypixelCommands.enderChest(page)
                            SearchableStorage.inventoryName = "Ender Chest ($page"
                        }

                        "Rift Storage" -> {
                            if (SkyBlockUtils.currentIsland == IslandType.THE_RIFT) HypixelCommands.enderChest(page)
                            SearchableStorage.inventoryName = "Rift Storage ($page"
                        }
                    }
                }
                if (storage.inventoryContainer.displayName == "Private Island Chest") {
                    SearchableStorage.waypoints = storage.inventoryContainer.run {
                        SoundUtils.createSound("random.orb", 1.0f).playSound()
                        ChatUtils.chat("Set waypoint to $displayName!")
                        SearchableStorage.inventoryName = "Chest"

                        listOfNotNull(primaryCords, secondaryCords)
                    }
                }

                SearchableStorage.highlightSlots = storage.visual.highlightSlots
            }
        }
    }

    private fun Pair<Renderable, List<Renderable>>?.handleClick(
        relativeMouseX: Int,
        relativeMouseY: Int,
        leftFunc: (Renderable) -> Int,
        leftAddFunc: (Renderable) -> Int,
        topFunc: (Renderable) -> Int,
        condition: (Int) -> Boolean,
        action: (Int) -> Unit,
    ) {
        this?.let { (modeRenderable, content) ->
            val top = topFunc(modeRenderable)
            var left = leftFunc(modeRenderable)
            println(top)
            println(left)

            content.forEachIndexed { i, renderable ->
                println(i)
                println(condition(i))
                println(renderable)
                println(left)
                println(GuiRenderUtils.isPointInRect(relativeMouseX, relativeMouseY, left, top, renderable.width, renderable.height))
                if (condition(i) &&
                    GuiRenderUtils.isPointInRect(relativeMouseX, relativeMouseY, left, top, renderable.width, renderable.height)
                ) {
                    println("test")
                    action(i)
                }
                left += leftAddFunc(renderable)
            }
        }
    }

    private fun getRelativeMousePos(): Pair<Int, Int> {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos
        return mouseX - guiLeft to mouseY - guiTop
    }

    private fun searchStorage(): List<List<Renderable>> {
        val matchingItems = mutableMapOf<NeuInternalName, ItemData>()

        StorageApi.accessStorage.forEach { (_, storage) ->
            val matching = if (search.isNotBlank()) storage.items.filterNotNull()
                .filter { searchMode.matches(it, search) } else storage.items.filterNotNull()
            if (matching.isEmpty()) return@forEach

            for (match in matching.groupBy { it.getInternalName() }) {
                val totalCount = match.value.sumOf { it.stackSize }

                val itemData = matchingItems.getOrPut(match.key) { ItemData(0, mutableListOf()) }
                itemData.amount += totalCount
                itemData.inventoryContainers += storage
            }
        }

        if (matchingItems.isEmpty()) return listOf()

        return createItemDisplay(matchingItems)
    }

    private fun createItemDisplay(items: Map<NeuInternalName, ItemData>, update: Boolean = true): List<List<Renderable>> {
        val sortedItems = sortMode.sort(items)
        val maxWidth = sortedItems.maxOf { StringRenderable(it.key.getItemStack().displayName).width }
        val usableWidth = (width * 0.8).toInt()
        val maxPerRow = (usableWidth / maxWidth).coerceAtLeast(1).coerceAtMost(6)

        val table = sortedItems.map { (internalName, data) ->
            val stack = internalName.getItemStack()
            val slotRenderable = Renderable.drawInsideRoundedRectWithOutline(
                Renderable.item(stack, 2.0),
                Color.decode("#8b8b8b"),
                radius = 0,
                topOutlineColor = Color.WHITE.rgb,
                bottomOutlineColor = Color.WHITE.rgb,
                borderOutlineThickness = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )

            Renderable.vertical(
                Renderable.placeholder(maxWidth),
                StringRenderable(stack.displayName, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                StringRenderable("§f${data.amount}x", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                slotRenderable,
            )
        }

        val maxHeight = table.maxOf { it.height }
        val usableHeight = (height * 0.6).toInt()
        val maxPerColumn = (usableHeight / maxHeight).coerceAtLeast(1).coerceAtMost(5)

        val displayedItemPositions = calculateItemPositions(table.chunked(maxPerRow), sortedItems)
        displayedItems = displayedItemPositions.entries.take(maxPerRow * maxPerColumn).associate { it.toPair() }
        pageColumns = maxPerRow
        pageRows = maxPerColumn
        if (update) allItems = displayedItemPositions
        if (update) selectedPage = 1

        return table.take(maxPerRow * maxPerColumn).chunked(maxPerRow)
    }

    private fun calculateItemPositions(
        table: List<List<Renderable>>,
        matchingItems: Map<NeuInternalName, ItemData>,
    ): Map<NeuInternalName, ItemRenderData> {
        val yOffsets = table.runningFold(headerHeight) { acc, row -> acc + row[0].height }.dropLast(1)
        val xOffsets = table.first().runningFold(0) { acc, item -> acc + item.width }.dropLast(1)

        val itemKeys = matchingItems.keys.toList()
        var flatIndex = 0

        return buildMap {
            for ((rowIndex, row) in table.withIndex()) {
                val y = yOffsets[rowIndex]
                for ((colIndex, renderable) in row.withIndex()) {
                    val x = xOffsets.getOrElse(colIndex) { 0 }

                    val internalName = itemKeys.getOrNull(flatIndex++) ?: continue
                    val data = matchingItems[internalName] ?: continue

                    put(internalName, ItemRenderData(data, renderable, x to y))
                }
            }
        }
    }


    private fun displayStorages(internalName: NeuInternalName): List<List<Renderable>> {
        val item = displayedItems[internalName] ?: return listOf()

        val storageVisualMap = item.data.inventoryContainers.mapNotNull { storage ->
            val highlightSlots = storage.items.withIndex()
                .filter { (_, item) -> item != null && item.getInternalName() == internalName }
                .map { it.index }

            if (highlightSlots.isEmpty()) return@mapNotNull null

            val renderable = storage.toRenderable(highlightSlots = highlightSlots)

            storage to StorageVisualData(renderable, highlightSlots)
        }.toMap()

        val table = storageVisualMap.values.map { it.renderable }.chunked(3)
        displayedStorages = calculateStoragePositions(table, storageVisualMap)

        return table
    }

    private fun calculateStoragePositions(
        table: List<List<Renderable?>>,
        storageVisualMap: Map<SkyHanniInventoryContainer, StorageVisualData>,
        xPadding: Int = 1,
        yPadding: Int = 0,
    ): List<StorageRenderData> {
        val rowHeights = RenderableUtils.calculateTableY(table, yPadding)

        val yOffsets = buildList {
            var y = headerHeight
            for (row in table) {
                add(y)
                y += rowHeights[row] ?: 0
            }
        }

        val xOffsets = RenderableUtils.calculateTableXOffsets(table, xPadding)

        return buildList {
            for ((rowIndex, row) in table.withIndex()) {
                val y = yOffsets[rowIndex]
                for ((colIndex, renderable) in row.withIndex()) {
                    if (renderable == null) continue

                    val x = xOffsets.getOrElse(colIndex) { 0 }

                    val (storage, visualData) = storageVisualMap.entries
                        .firstOrNull { it.value.renderable == renderable } ?: continue

                    add(StorageRenderData(storage, StorageVisualData(renderable, visualData.highlightSlots), x to y))
                }
            }
        }
    }
}
