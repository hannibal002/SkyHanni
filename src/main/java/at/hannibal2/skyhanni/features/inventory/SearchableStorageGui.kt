package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.api.StorageApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.features.inventory.SearchableStorage.storagePattern
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
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
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard
import java.awt.Color

class SearchableStorageGui : SkyhanniBaseScreen() {

    private var search = ""
    private var searchMode = SearchMode.NAME
    private var displayedStorages = listOf<StorageRenderData>()

    private var headerHeight = 0
    private var guiLeft = 0
    private var guiTop = 0
    private var display: Renderable? = null
    private var searchModeRenderable: Renderable? = null

    data class StorageRenderData(
        val inventoryContainer: SkyHanniInventoryContainer,
        val visual: StorageVisualData,
        val position: Pair<Int, Int>,
    )

    data class StorageVisualData(
        val renderable: Renderable,
        val highlightSlots: List<Int>,
    )

    enum class SearchMode {
        NAME,
        LORE;

        fun next(): SearchMode = when (this) {
            NAME -> LORE
            LORE -> NAME
        }
    }

    override fun guiClosed() {
        OtherInventoryData.close("Searchable Storage GUI")
    }

    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        drawDefaultBackground(originalMouseX, originalMouseY, partialTicks)

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
            search.isBlank() -> RenderableString(
                "§aSearch something!",
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )

            else -> buildSearch()
        }

        return VerticalContainerRenderable(listOf(header, Renderable.placeholder(1), content))
    }

    private fun buildHeader(): Renderable {
        val searchField = RenderableString(
            "§eSearch: §7$search",
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        )

        val searchModeField = RenderableString(
            "§eSearch by: §7${searchMode.name}",
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        )
        searchModeRenderable = searchModeField

        return VerticalContainerRenderable(listOf(searchField, searchModeField)).also {
            headerHeight = it.height + 10
        }
    }

    private fun buildSearch(): Renderable {
        val results = searchStorage()
        return if (results.isEmpty()) {
            RenderableString(
                "§cNothing found :( Did you open all your storages?",
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        } else {
            Renderable.table(results)
        }
    }

    override fun onKeyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_BACK -> if (search.isNotEmpty()) search = search.dropLast(1)
            else -> if (typedChar.isLetterOrDigit() || typedChar in listOf(' ', '_', '-')) {
                search += typedChar
            }
        }
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        val (relativeMouseX, relativeMouseY) = getRelativeMousePos()

        searchModeRenderable?.let { modeRenderable ->
            val left = 0
            val top = modeRenderable.height

            val isHovered =
                GuiRenderUtils.isPointInRect(relativeMouseX, relativeMouseY, left, top, modeRenderable.width, modeRenderable.height)

            if (isHovered) toggleSearchMode()
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
                        ChatUtils.chat("Set waypoint to ${displayName}!")
                        SearchableStorage.inventoryName = "Island Chest"

                        listOfNotNull(primaryCords, secondaryCords)
                    }
                }

                SearchableStorage.highlightSlots = storage.visual.highlightSlots
            }
        }
    }

    private fun getRelativeMousePos(): Pair<Int, Int> {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos
        return mouseX - guiLeft to mouseY - guiTop
    }

    private fun toggleSearchMode() {
        searchMode = searchMode.next()
    }

    private fun searchStorage(): List<List<Renderable>> {
        val storageVisualMap = StorageApi.accessStorage.mapNotNull { (_, storage) ->
            val highlightSlots = storage.items.withIndex()
                .filter { (_, item) -> item?.matches() == true }
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

    private fun ItemStack.matches(): Boolean = when (searchMode) {
        SearchMode.NAME -> displayName.removeColor().contains(search, ignoreCase = true)
        SearchMode.LORE -> getLore().any { it.removeColor().contains(search, ignoreCase = true) }
    }
}
