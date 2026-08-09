package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractWardrobeApi.Companion.MAX_PAGES
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractWardrobeApi.Companion.MAX_SLOT_PER_PAGE
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.clickSlot
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.AbstractCustomMenuScreen
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.CustomRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import java.awt.Color
import kotlin.math.min

class CustomWardrobeScreen(
    menu: ChestMenu,
    title: Component,
) : AbstractCustomMenuScreen(menu, title) {
    private var updateScheduled = false

    private var displayRenderable: Renderable? = null
    internal var waitingForInventoryUpdate = false

    private val position: Position = Position().ignoreScale()
    private val loadingPosition: Position = Position().ignoreScale()

    private var activeScale: Double = 1.0
    private var currentMaxSize: Pair<Int, Int>? = null
    private var lastScreenSize: Pair<Int, Int>? = null

    var renderableTopCorner: Pair<Int, Int> = 0 to 0
        private set
    var renderableDimensions: Pair<Int, Int> = 0 to 0
        private set

    override fun getRectangle(): ScreenRectangle = ScreenRectangle(
        renderableTopCorner.first,
        renderableTopCorner.second,
        renderableDimensions.first,
        renderableDimensions.second,
    )

    override fun shouldShowItemList(): Boolean = config.showReiItems

    override fun isSwitchingScreens(): Boolean = CustomWardrobe.switchingScreens

    override fun onInitGui() {
        CustomWardrobe.switchingScreens = false
        updateScreenSize(width to height)
        // slotChanged is called when a screen is opened, so no need to call CustomWardrobe.onInventoryUpdate() here
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderWardrobeOverlay(this.width, this.height)
    }

    override fun slotChanged(container: AbstractContainerMenu, slotId: Int, stack: SafeItemStack) {
        if (updateScheduled) return
        updateScheduled = true

        DelayedRun.runNextTick {
            updateScheduled = false
            onInventoryUpdate()
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        CustomWardrobeKeybinds.handlePress()
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        CustomWardrobeKeybinds.handlePress()
    }

    fun renderWardrobeOverlay(screenWidth: Int, screenHeight: Int) {
        val screenSize = screenWidth to screenHeight

        if (screenSize != lastScreenSize) {
            lastScreenSize = screenSize
            if (updateScreenSize(screenSize)) {
                return
            }
        }

        val renderable = displayRenderable ?: return

        if (waitingForInventoryUpdate && config.loadingText) {
            val loadingRenderable = Renderable.text(
                "§cLoading...",
                scale = activeScale,
            )
            loadingPosition
                .moveTo(
                    position.x + (renderable.width - loadingRenderable.width) / 2,
                    position.y - loadingRenderable.height,
                )
                .renderRenderable(
                    loadingRenderable,
                    posLabel = CustomWardrobe.GUI_NAME,
                    addToGuiManager = false,
                )
        }

        DrawContextUtils.translatedPushPopResult(0f, 0f) {
            position.renderRenderable(renderable, posLabel = CustomWardrobe.GUI_NAME, addToGuiManager = false)
            if (EstimatedItemValue.config.enabled) {
                DrawContextUtils.translate(0f, 0f)
                EstimatedItemValue.tryRendering()
            }
        }
    }

    internal fun onInventoryUpdate() {
        waitingForInventoryUpdate = false
        update()
    }

    internal fun update() {
        displayRenderable = createRenderables()
    }

    internal fun updateScreenSize(gui: Pair<Int, Int>): Boolean {
        val renderable = currentMaxSize ?: run {
            activeScale = config.spacing.globalScale.get() / 100.0
            update()
            updateRenderablePosition(gui)
            return true
        }
        val previousActiveScale = activeScale
        val unscaledRenderableWidth = renderable.first / activeScale
        val unscaledRenderableHeight = renderable.second / activeScale
        val autoScaleWidth = 0.95 * gui.first / unscaledRenderableWidth
        val autoScaleHeight = 0.95 * gui.second / unscaledRenderableHeight
        val maxScale = min(autoScaleWidth, autoScaleHeight)

        activeScale = (config.spacing.globalScale.get() / 100.0).coerceAtMost(maxScale)

        if (activeScale != previousActiveScale) {
            update()
        }

        updateRenderablePosition(gui)

        return activeScale != previousActiveScale
    }

    private fun updateRenderablePosition(gui: Pair<Int, Int>) {
        val renderable = displayRenderable ?: return

        val (width, height) = renderable.width to renderable.height
        renderableDimensions = width to height

        val left = (gui.first - width) / 2
        val top = (gui.second - height) / 2

        position.moveTo(left, top)
        renderableTopCorner = left to top
    }

    private fun createWarning(list: List<WardrobeSlot>): Pair<String?, List<WardrobeSlot>> {
        var wardrobeWarning: String? = null
        var wardrobeSlots = list

        if (wardrobeSlots.isEmpty()) wardrobeWarning = "§cYour wardrobe is empty :("

        if (config.hideLockedSlots) {
            wardrobeSlots = wardrobeSlots.filter { !it.locked }
            if (wardrobeSlots.isEmpty()) wardrobeWarning = "§cAll your slots are locked? Somehow"
        }

        if (config.hideEmptySlots) {
            wardrobeSlots = wardrobeSlots.filter { !it.isEmpty() }
            if (wardrobeSlots.isEmpty()) wardrobeWarning = "§cAll slots are empty :("
        }
        if (config.onlyFavorites) {
            wardrobeSlots = wardrobeSlots.filter { it.favorite || it.isCurrentSlot() }
            if (wardrobeSlots.isEmpty()) wardrobeWarning = "§cDidn't set any favorites"
        }

        return wardrobeWarning to wardrobeSlots
    }

    private fun createRenderables(): Renderable {
        val (wardrobeWarning, list) = createWarning(ArmorWardrobeApi.slots)

        val maxPlayersPerRow = config.spacing.maxPlayersPerRow.get().coerceAtLeast(1)
        val maxPlayersRows = ((MAX_SLOT_PER_PAGE * MAX_PAGES - 1) / maxPlayersPerRow) + 1
        val containerWidth = (config.spacing.slotWidth.get() * activeScale).toInt()
        val containerHeight = (config.spacing.slotHeight.get() * activeScale).toInt()
        val playerWidth = (containerWidth * (config.spacing.playerScale.get() / 100.0))
        val horizontalSpacing = (config.spacing.horizontalSpacing.get() * activeScale).toInt()
        val verticalSpacing = (config.spacing.verticalSpacing.get() * activeScale).toInt()
        val backgroundPadding = (config.spacing.backgroundPadding.get() * activeScale).toInt()
        val buttonVerticalSpacing = (config.spacing.buttonVerticalSpacing.get() * activeScale).toInt()

        var maxRenderableWidth = maxPlayersPerRow * containerWidth + (maxPlayersPerRow - 1) * horizontalSpacing
        var maxRenderableHeight = maxPlayersRows * containerHeight + (maxPlayersRows - 1) * verticalSpacing

        val button = addButtons()

        if (button.width > maxRenderableWidth) maxRenderableWidth = button.width
        maxRenderableHeight += button.height + buttonVerticalSpacing

        maxRenderableWidth += 2 * backgroundPadding
        maxRenderableHeight += 2 * backgroundPadding
        currentMaxSize = maxRenderableWidth to maxRenderableHeight

        wardrobeWarning?.let { text ->
            val warningRenderable = Renderable.wrappedText(
                text,
                maxRenderableWidth,
                3.0 * activeScale,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
            val withButtons = Renderable.vertical(
                warningRenderable,
                button,
                spacing = buttonVerticalSpacing,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
            return addGuiBackground(withButtons, backgroundPadding)
        }

        val chunkedList = list.chunked(maxPlayersPerRow)

        val rowsRenderables = chunkedList.map { row ->
            val slotsRenderables = row.map { slot ->
                val armorTooltipRenderable = CustomRenderUtils.createArmorTooltipRenderable(
                    slot.armor,
                    containerHeight,
                    containerWidth,
                    config.tooltipKeybind,
                )
                val (topOutline, bottomOutline) = slot.getOutlineColor()

                val playerBackground = CustomRenderUtils.createHoverableRenderable(
                    armorTooltipRenderable,
                    topLayerRenderable = addSlotHoverableButtons(slot),
                    hoveredColor = slot.getSlotColor(),
                    borderOutlineThickness = config.spacing.outlineThickness.get(),
                    borderOutlineBlur = config.spacing.outlineBlur.get(),
                    onClick = { slot.clickSlot() },
                    topOutlineColor = topOutline,
                    bottomOutlineColor = bottomOutline,
                )

                val playerRenderable = CustomRenderUtils.createFakePlayerRenderable(
                    slot.armor,
                    slot.isInCurrentPage(),
                    playerWidth,
                    containerHeight,
                    containerWidth
                )

                Renderable.doubleLayered(
                    playerBackground,
                    playerRenderable,
                    blockBottomHover = false,
                    forceBottomRenderFirst = true
                )
            }
            Renderable.horizontal(slotsRenderables, horizontalSpacing)
        }

        val allSlotsRenderable = Renderable.vertical(
            rowsRenderables,
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        val withButtons = Renderable.vertical(
            listOf(allSlotsRenderable, button),
            buttonVerticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        return addGuiBackground(withButtons, backgroundPadding)
    }

    private fun addGuiBackground(renderable: Renderable, borderPadding: Int) =
        CustomRenderUtils.addGuiBackground(
            renderable = renderable,
            borderPadding = borderPadding,
            scale = activeScale,
            backgroundColor = config.color.backgroundColor,
            onLeftClick = {
                config::enabled.jumpToEditor()
                reset()
                ArmorWardrobeApi.currentPage = null
            },
        )

    internal fun reset() {
        displayRenderable = null
    }

    private fun addButtons(): Renderable {
        val (horizontalSpacing, verticalSpacing) = with(config.spacing) {
            buttonHorizontalSpacing.get() * activeScale to buttonVerticalSpacing.get() * activeScale
        }

        val backButton = CustomWardrobe.createLabeledButton(
            "§aBack",
            onClick = {
                InventoryUtils.clickSlot(48)
                reset()
                ArmorWardrobeApi.currentPage = null
            },
            scale = activeScale,
        )
        val exitButton = CustomWardrobe.createLabeledButton(
            "§cClose",
            onClick = {
                InventoryUtils.clickSlot(49)
                reset()
                ArmorWardrobeApi.currentPage = null
            },
            scale = activeScale,
        )

        val greenColor = Color(85, 255, 85, 200)
        val redColor = Color(255, 85, 85, 200)

        val onlyFavoriteButton = CustomWardrobe.createLabeledButton(
            "§eFavorite",
            hoveredColor = if (config.onlyFavorites) greenColor else redColor,
            onClick = {
                config.onlyFavorites = !config.onlyFavorites
                update()
            },
            scale = activeScale,
        )

        val editButton = CustomWardrobe.createLabeledButton(
            "§bEdit",
            onClick = {
                DelayedRun.runNextTick {
                    reset()
                    CustomWardrobe.enterEditMode()
                }
            },
            scale = activeScale,
        )

        val row = Renderable.horizontal(
            backButton,
            exitButton,
            onlyFavoriteButton,
            spacing = horizontalSpacing.toInt(),
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        val total = Renderable.vertical(
            row,
            editButton,
            spacing = verticalSpacing.toInt(),
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        )

        return total
    }

    private fun addSlotHoverableButtons(wardrobeSlot: WardrobeSlot): Renderable {
        val textScale = 1.5 * activeScale
        val shouldRender = !wardrobeSlot.isEmpty() && !wardrobeSlot.locked
        if (!shouldRender && !wardrobeSlot.favorite) return Renderable.placeholder(0, 0)
        val list = buildList {
            add(
                Renderable.clickable(
                    Renderable.hoverable(
                        CustomRenderUtils.centerString((if (wardrobeSlot.favorite) "§c" else "§7") + "❤", scale = textScale),
                        CustomRenderUtils.centerString((if (wardrobeSlot.favorite) "§4" else "§8") + "❤", scale = textScale),
                    ),
                    onLeftClick = {
                        wardrobeSlot.favorite = !wardrobeSlot.favorite
                        update()
                    },
                ),
            )
            if (config.estimatedValue && shouldRender) {
                add(
                    Renderable.hoverTips(
                        CustomRenderUtils.centerString("§2$", scale = textScale),
                        ArmorWardrobeApi.createPriceLore(wardrobeSlot),
                    ),
                )
            }
        }

        return Renderable.vertical(list, 1, HorizontalAlignment.RIGHT)
    }

    companion object {
        private val config get() = CustomWardrobe.config

        private fun WardrobeSlot.getOutlineColor(): Pair<Color, Color> {
            val (top, bottom) = config.color.topBorderColor.toColor() to config.color.bottomBorderColor.toColor()
            return when {
                isEmpty() || locked -> ColorUtils.TRANSPARENT_COLOR to ColorUtils.TRANSPARENT_COLOR
                !isInCurrentPage() -> top.darker(0.5) to bottom.darker(0.5)
                else -> top to bottom
            }
        }

        private fun WardrobeSlot.getSlotColor(): Color = with(config.color) {
            when {
                isCurrentSlot() -> equippedColor
                favorite && !config.onlyFavorites -> favoriteColor
                else -> null
            }?.toColor()?.transformIf({ !isInCurrentPage() }) { darker() }
                ?: (if (isInCurrentPage()) samePageColor else otherPageColor).toColor()
                    .transformIf({ locked || isEmpty() }) { darker(0.2) }.addAlpha(100)
        }
    }
}
