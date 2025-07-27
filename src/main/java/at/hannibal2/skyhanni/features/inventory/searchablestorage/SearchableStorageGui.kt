package at.hannibal2.skyhanni.features.inventory.searchablestorage

import at.hannibal2.skyhanni.api.StorageApi
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.searchMode
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.sortMode
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorage.storagePattern
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.buildFooter
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.buildHeader
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.buildSearch
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.buildTooltip
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.footerRenderable
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.handleClick
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.handleIslandChestClick
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.handlePageSelection
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.handleStorageClick
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.searchModeRenderable
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.sortModeRenderable
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard
import java.awt.Color

class SearchableStorageGui(private var search: String = "") : SkyhanniBaseScreen() {

    private var display: Renderable? = null
    private var tooltipRenderable: Renderable? = null

    private var displayedItems = listOf<ItemRenderData>()
    private var allItems = listOf<ItemRenderData>()
    private var selectedItem = NeuInternalName.NONE
    private var displayedStorages = listOf<StorageRenderData>()
    private var allStorages = listOf<StorageRenderData>()
    private var guiLeft = 0
    private var guiTop = 0

    data class StorageRenderData(
        val inventoryContainer: SkyHanniInventoryContainer,
        val visual: StorageVisualData,
        val position: RenderPosition,
    )

    data class StorageVisualData(
        val renderable: Renderable,
        val highlightSlots: List<Int>,
    )

    data class ItemRenderData(
        val internalName: NeuInternalName,
        val data: ItemData,
        val renderable: Renderable,
        val position: RenderPosition,
    )

    data class ItemData(
        val stack: ItemStack,
        var amount: Int,
        val inventories: MutableList<SkyHanniInventoryContainer>,
    )

    data class RenderPosition(
        val x: Int,
        val y: Int,
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
                if (displayedItems.isNotEmpty()) {
                    val item = displayedItems.first().renderable
                    val (relativeMouseX, relativeMouseY) = getRelativeMousePos()
                    DrawContextUtils.translate(relativeMouseX.toFloat() + (item.width / 2), relativeMouseY.toFloat() - item.height, 0f)
                    tooltipRenderable?.render(0, 0)
                }
            }
        }
    }

    private fun buildContent(): Renderable {
        val header = buildHeader(search)

        val content = when {
            search.isBlank() && displayedItems.isEmpty() -> buildSearch(searchStorage())
            search.isNotBlank() && displayedItems.isEmpty() -> buildSearch(searchStorage())
            selectedItem != NeuInternalName.NONE && displayedStorages.isEmpty() -> buildSearch(displayStorages())
            selectedItem != NeuInternalName.NONE && displayedStorages.isNotEmpty() -> buildSearch(
                createStorageDisplay(displayedStorages.associate { it.inventoryContainer to it.visual }),
            )

            else -> buildSearch(createItemDisplay(displayedItems.associate { it.internalName to it.data }, update = false))
        }

        val footer = buildFooter(if (selectedItem != NeuInternalName.NONE) allStorages.size else allItems.size)

        val placeholder = Renderable.placeholder(1)

        return Renderable.vertical(listOf(header, placeholder, content, placeholder, footer))
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (displayedStorages.isNotEmpty()) {
                    displayedStorages = listOf()
                    selectedItem = NeuInternalName.NONE
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
        if (displayedStorages.isNotEmpty()) return

        val item = displayedItems.firstOrNull {
            GuiRenderUtils.isPointInRect(
                relativeMouseX, relativeMouseY,
                it.position.x, it.position.y,
                it.renderable.width, it.renderable.height,
            )
        } ?: return


        tooltipRenderable = buildTooltip(item)
        return
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        val (relativeMouseX, relativeMouseY) = getRelativeMousePos()

        searchModeRenderable.handleClick(relativeMouseX, relativeMouseY, { it.height * 1 }, { it % 2 != 0 }) { index ->
            searchMode = SearchMode.entries.getOrNull((index - 1) / 2) ?: searchMode
        }

        sortModeRenderable.handleClick(relativeMouseX, relativeMouseY, { it.height * 2 }, { it % 2 != 0 }) { index ->
            sortMode = SortMode.entries.getOrNull((index - 1) / 2) ?: sortMode
        }

        val before = SearchableStorageUtil.selectedPage
        footerRenderable.handleClick(
            relativeMouseX, relativeMouseY,
            { (display?.height ?: 0) - it.height },
            { it != 1 },
            { ((display?.width ?: 0) - it.width) / 2 },
            spacing = 15,
        ) { index ->
            val itemSelected = selectedItem != NeuInternalName.NONE
            val content = if (itemSelected) allStorages else allItems
            val (start, end) = index.handlePageSelection(content)

            if (!itemSelected) displayedItems = allItems.subList(start, end)
            else displayedStorages = allStorages.subList(start, end)
        }

        if (before != SearchableStorageUtil.selectedPage || mouseButton != 0) return

        if (displayedStorages.isEmpty()) {
            val item = displayedItems.firstOrNull {
                GuiRenderUtils.isPointInRect(
                    relativeMouseX, relativeMouseY,
                    it.position.x, it.position.y,
                    it.renderable.width, it.renderable.height,
                )
            } ?: return

            selectedItem = item.internalName
        } else {
            val storage = displayedStorages.firstOrNull {
                GuiRenderUtils.isPointInRect(
                    relativeMouseX, relativeMouseY,
                    it.position.x, it.position.y,
                    it.visual.renderable.width, it.visual.renderable.height,
                )
            } ?: return

            SearchableStorage.highlightSlots = storage.visual.highlightSlots
            val name = storage.inventoryContainer.displayName

            when {
                storagePattern.matches(name) -> storage.handleStorageClick()
                name == "Private Island Chest" -> storage.handleIslandChestClick()
            }
        }
    }

    private fun searchStorage(): List<List<Renderable>> {
        val matchingItems = mutableMapOf<NeuInternalName, ItemData>()

        StorageApi.accessStorage.forEach { (_, storage) ->
            val matches = storage.items.filterNotNull()
                .filter { search.isBlank() || searchMode.matches(it, search) }

            if (matches.isEmpty()) return@forEach

            matches.groupBy { it.getInternalName() }.forEach { (name, grouped) ->
                val itemData = matchingItems.getOrPut(name) {
                    ItemData(grouped.first(), 0, mutableListOf())
                }
                itemData.amount += grouped.sumOf { it.stackSize }
                itemData.inventories += storage
            }
        }

        return if (matchingItems.isEmpty()) emptyList() else createItemDisplay(matchingItems)
    }

    private fun displayStorages(): List<List<Renderable>> {
        val item = displayedItems.firstOrNull { it.internalName == selectedItem } ?: return emptyList()

        val storageVisuals = item.data.inventories.mapNotNull { storage ->
            val highlightSlots = storage.items.withIndex()
                .filter { (_, item) -> item?.getInternalName() == selectedItem }
                .map { it.index }

            if (highlightSlots.isEmpty()) return@mapNotNull null

            storage to StorageVisualData(storage.toRenderable(highlightSlots = highlightSlots), highlightSlots)
        }.toMap()

        return if (storageVisuals.isEmpty()) listOf() else createStorageDisplay(storageVisuals)
    }

    private fun createItemDisplay(
        items: Map<NeuInternalName, ItemData>,
        update: Boolean = true,
    ): List<List<Renderable>> {
        return SearchableStorageUtil.createDisplay(
            width, height,
            data = sortMode.sort(items),
            renderableFn = { internalName, itemData ->
                Renderable.vertical(
                    SearchableStorageUtil.fakeSlotRenderable(internalName),
                    StringRenderable("§f${itemData.amount}x", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                )
            },
            calculateFn = ::calculateItemPositions,
            updateFn = { displayedList, visibleCount ->
                val displayed = displayedList.filterIsInstance<ItemRenderData>()
                displayedItems = displayed.take(visibleCount)
                if (update) {
                    allItems = displayed
                    SearchableStorageUtil.selectedPage = 1
                }
            },
        )
    }

    private fun createStorageDisplay(
        storages: Map<SkyHanniInventoryContainer, StorageVisualData>,
        update: Boolean = true,
    ): List<List<Renderable>> = SearchableStorageUtil.createDisplay(
        width, height,
        data = storages,
        renderableFn = { _, visual -> visual.renderable },
        calculateFn = ::calculateStoragePositions,
        updateFn = { displayedList, visibleCount ->
            val displayed = displayedList.filterIsInstance<StorageRenderData>()
            displayedStorages = displayed.take(visibleCount)
            if (update) {
                allStorages = displayed
                SearchableStorageUtil.selectedPage = 1
            }
        },
    )

    private fun calculateItemPositions(
        table: List<List<Renderable>>,
        items: Map<NeuInternalName, ItemData>,
        xPadding: Int = 1,
    ): List<ItemRenderData> {
        var index = 0
        return SearchableStorageUtil.calculateRenderPositions(
            table = table,
            dataMap = items,
            matchFn = { _, map ->
                val key = map.keys.elementAtOrNull(index++)
                key?.let { k -> map[k]?.let { v -> k to v } }
            },
            buildFn = { key, data, renderable, pos ->
                ItemRenderData(key, data, renderable, pos)
            },
            xPadding = xPadding,
            useDynamicRowHeight = false,
        )
    }

    private fun calculateStoragePositions(
        table: List<List<Renderable>>,
        storages: Map<SkyHanniInventoryContainer, StorageVisualData>,
        xPadding: Int = 1,
        yPadding: Int = 0,
    ): List<StorageRenderData> = SearchableStorageUtil.calculateRenderPositions(
        table,
        dataMap = storages,
        matchFn = { renderable, map ->
            map.entries.firstOrNull { it.value.renderable == renderable }?.let { it.key to it.value }
        },
        buildFn = { key, data, _, pos ->
            StorageRenderData(key, data, pos)
        },
        xPadding,
        yPadding,
        useDynamicRowHeight = true,
    )

    private fun getRelativeMousePos(): Pair<Int, Int> {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos
        return mouseX - guiLeft to mouseY - guiTop
    }
}
