package at.hannibal2.skyhanni.features.inventory.searchablestorage

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.storagePattern
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageGui.SearchMode
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageGui.SortMode
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import java.awt.Color

object SearchableStorageUtil {

    var searchModeRenderable: Pair<Renderable, List<Renderable>>? = null
    var sortModeRenderable: Pair<Renderable, List<Renderable>>? = null
    var footerRenderable: Pair<Renderable, List<Renderable>>? = null

    var selectedPage = 0
    private var pageSize = 0
    private var headerHeight = 0

    fun buildHeader(search: String): Renderable {
        val searchField = StringRenderable(
            "§eSearch: §7$search",
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        )

        val searchMode = buildModeField("§eSearch by:", SearchMode.entries)
        searchModeRenderable = searchMode

        val sortMode = buildModeField("§eSort by:", SortMode.entries)
        sortModeRenderable = sortMode

        return Renderable.vertical(listOf(searchField, searchMode.first, sortMode.first)).also { headerHeight = it.height + 10 }
    }

    private fun buildModeField(
        label: String,
        entries: List<Any>,
    ): Pair<Renderable, List<StringRenderable>> {
        val separator = StringRenderable("§e|")
        val renderables = listOf(StringRenderable("§e$label")) +
            entries.map { StringRenderable(it.toString()) }
                .flatMapIndexed { i, renderable -> if (i < entries.size - 1) listOf(renderable, separator) else listOf(renderable) }

        val field = Renderable.horizontal(
            renderables,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        )
        return field to renderables
    }

    fun Pair<Renderable, List<Renderable>>?.handleClick(
        relativeMouseX: Int,
        relativeMouseY: Int,
        topOffset: (Renderable) -> Int,
        condition: (Int) -> Boolean,
        leftOffset: (Renderable) -> Int = { 0 },
        spacing: Int = 0,
        action: (Int) -> Unit,
    ) {
        this?.let { (modeRenderable, content) ->
            val top = topOffset(modeRenderable)
            var left = leftOffset(modeRenderable)

            content.forEachIndexed { i, renderable ->
                if (condition(i) &&
                    GuiRenderUtils.isPointInRect(relativeMouseX, relativeMouseY, left, top, renderable.width, renderable.height)
                ) {
                    action(i)
                }
                left += renderable.width + spacing
            }
        }
    }

    fun SearchableStorageGui.StorageRenderData.handleStorageClick() = storagePattern.matchMatcher(this.inventoryContainer.displayName) {
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

    fun SearchableStorageGui.StorageRenderData.handleIslandChestClick() {
        SearchableStorage.waypoints = with(this.inventoryContainer) {
            SoundUtils.createSound("random.orb", 1.0f).playSound()
            ChatUtils.chat("Set waypoint to $displayName!")
            SearchableStorage.inventoryName = "Chest"

            listOfNotNull(primaryCords, secondaryCords)
        }
    }

    fun Int.handlePageSelection(
        content: List<Any>,
    ): Pair<Int, Int> {
        if (content.isEmpty()) return 0 to 0
        val pageCount = calculatePageCount(content.size, pageSize)
        selectedPage = when (this) {
            0 -> if (selectedPage <= 1) pageCount else selectedPage - 1
            2 -> if (selectedPage >= pageCount) 1 else selectedPage + 1
            else -> selectedPage
        }
        val start = (selectedPage - 1) * pageSize
        val end = (start + pageSize).coerceAtMost(content.size)

        return start to end
    }

    fun buildSearch(results: List<List<Renderable>>): Renderable {
        return if (results.isEmpty()) {
            StringRenderable(
                "§cNothing found :( Did you open all your storages?", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )
        } else {
            Renderable.table(results)
        }
    }

    fun buildFooter(
        contentSize: Int,
    ): Renderable {
        val maxPages = if (contentSize <= 0) 0 else calculatePageCount(contentSize, pageSize)

        val content = listOf(
            minecraftButtonRenderable("<"),
            StringRenderable("§f$selectedPage§7/§f$maxPages"),
            minecraftButtonRenderable(">"),
        )

        return Renderable.horizontal(content, spacing = 15, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER).also {
            footerRenderable = it to content
        }
    }

    fun buildTooltip(
        item: SearchableStorageGui.ItemRenderData,
    ): Renderable {
        val backgroundColor = Color.decode("#100110")
        val borderColor = Color.decode("#21014f")

        val lore = item.data.stack.getLore()
        val name = item.data.stack.displayName

        val tooltipText = Renderable.vertical {
            add(StringRenderable(name))
            lore.forEach { add(StringRenderable(it)) }
        }

        return Renderable.drawInsideRoundedRect(
            Renderable.drawInsideRoundedRectWithOutline(
                tooltipText,
                backgroundColor,
                radius = 0,
                topOutlineColor = borderColor.rgb,
                bottomOutlineColor = borderColor.rgb,
                borderOutlineThickness = 1,
            ),
            backgroundColor,
            radius = 1,
            padding = 0,
        )
    }

    fun minecraftButtonRenderable(text: String) = drawInsideMinecraftRect(StringRenderable(text))

    fun fakeSlotRenderable(internalName: NeuInternalName, scale: Double = 2.0) =
        drawInsideMinecraftRect(Renderable.item(internalName, scale))

    private fun drawInsideMinecraftRect(input: Renderable) = Renderable.drawInsideRoundedRectWithOutline(
        input,
        Color.decode("#8b8b8b"),
        radius = 0,
        topOutlineColor = Color.WHITE.rgb,
        bottomOutlineColor = Color.WHITE.rgb,
        borderOutlineThickness = 2,
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    fun <T, D> createDisplay(
        screenWidth: Int,
        screenHeight: Int,
        data: Map<T, D>,
        renderableFn: (T, D) -> Renderable,
        calculateFn: (List<List<Renderable>>, Map<T, D>) -> List<*>,
        updateFn: (List<*>, Int) -> Unit,
    ): List<List<Renderable>> {
        val renderables = data.map { (key, value) -> renderableFn(key, value) }

        val (maxPerRow, maxPerColumn) = calculateRowsAndColumns(
            screenWidth, screenHeight, renderables.maxOf { it.width }, renderables.maxOf { it.height },
        )

        val visibleCount = maxPerRow * maxPerColumn
        val table = renderables.chunked(maxPerRow)
        val positioned = calculateFn(table, data)

        pageSize = visibleCount
        updateFn(positioned, visibleCount)

        return table.take(visibleCount)
    }

    fun <T, D, R> calculateRenderPositions(
        table: List<List<Renderable>>,
        dataMap: Map<T, D>,
        matchFn: (Renderable, Map<T, D>) -> Pair<T, D>?, // For lookup
        buildFn: (T, D, Renderable, SearchableStorageGui.RenderPosition) -> R,
        xPadding: Int = 1,
        yPadding: Int = 0,
        useDynamicRowHeight: Boolean = false,
    ): List<R> {
        val yOffsets = if (useDynamicRowHeight) {
            val rowHeights = RenderableUtils.calculateTableY(table, yPadding)
            table.indices.runningFold(headerHeight) { acc, i -> acc + (rowHeights[table[i]] ?: 0) }.dropLast(1)
        } else {
            table.runningFold(headerHeight) { acc, row -> acc + row.first().height }.dropLast(1)
        }

        val xOffsets = RenderableUtils.calculateTableXOffsets(table, xPadding)

        var index = 0
        return buildList {
            for ((rowIndex, row) in table.withIndex()) {
                val y = yOffsets.getOrElse(rowIndex) { headerHeight }
                for ((colIndex, renderable) in row.withIndex()) {
                    val x = xOffsets.getOrElse(colIndex) { 0 }
                    val pos = SearchableStorageGui.RenderPosition(x, y)

                    val (key, data) = matchFn(renderable, dataMap) ?: continue
                    add(buildFn(key, data, renderable, pos))
                    index++
                }
            }
        }
    }

    private fun calculatePageCount(contentSize: Int, pageSize: Int): Int = (contentSize + (pageSize - 1)) / pageSize

    private fun calculateRowsAndColumns(screenWidth: Int, screenHeight: Int, maxItemWidth: Int, maxItemHeight: Int): Pair<Int, Int> {
        val usableWidth = (screenWidth * 0.8).toInt()
        val usableHeight = (screenHeight * 0.6).toInt()

        return (usableWidth / maxItemWidth).coerceAtLeast(1) to (usableHeight / maxItemHeight).coerceAtLeast(1)
    }
}
