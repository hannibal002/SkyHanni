package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.inventory.customwardrobe.CustomWardrobeConfig
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractWardrobeApi.Companion.MAX_PAGES
import at.hannibal2.skyhanni.features.inventory.wardrobe.AbstractWardrobeApi.Companion.MAX_SLOT_PER_PAGE
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.GUI_NAME
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.clickSlot
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
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
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
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
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.min

@Suppress("TooManyFunctions", "LongMethod")
class CustomWardrobeScreen(
    menu: ChestMenu,
    title: Component,
) : AbstractCustomMenuScreen(menu, title) {
    private var displayRenderable: Renderable? = null
    var waitingForInventoryUpdate = false
        private set
    private var updateScheduled = false

    private val position: Position = Position().ignoreScale()
    private val loadingPosition: Position = Position().ignoreScale()

    private var activeScale: Int = 100
    private var currentMaxSize: Pair<Int, Int>? = null
    private var lastScreenSize: Pair<Int, Int>? = null

    private var renderableTopCorner: Pair<Int, Int> = 0 to 0
    private var renderableDimensions: Pair<Int, Int> = width to height

    override fun getRectangle(): ScreenRectangle = ScreenRectangle(
        renderableTopCorner.first,
        renderableTopCorner.second,
        renderableDimensions.first,
        renderableDimensions.second
    )
    override fun shouldShowItemList(): Boolean = config.showReiItems

    override fun onInitGui() {
        CustomWardrobe.switchingScreens = false
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderWardrobeOverlay(this.width, this.height)
    }

    override fun slotChanged(container: AbstractContainerMenu, slotId: Int, stack: SafeItemStack) {
        if (updateScheduled) return
        updateScheduled = true

        DelayedRun.runNextTick {
            waitingForInventoryUpdate = false
            updateScheduled = false
            update()
        }
    }

    override fun removed() {
        reset()
        val player = MinecraftCompat.localPlayerOrNull ?: return
        if (!CustomWardrobe.switchingScreens) {
            menu.removed(player)
        }
        menu.removeSlotListener(this)
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        CustomWardrobeKeybinds.handlePress()
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        CustomWardrobeKeybinds.handlePress()
    }

    private fun renderWardrobeOverlay(screenWidth: Int, screenHeight: Int) {
        val renderable = displayRenderable ?: run {
            update()
            displayRenderable ?: return
        }

        val screenSize = screenWidth to screenHeight

        if (screenSize != lastScreenSize) {
            lastScreenSize = screenSize
            val shouldUpdate = updateScreenSize(screenSize)
            if (shouldUpdate) {
                update()
                return
            }
        }

        val (width, height) = renderable.width to renderable.height
        renderableDimensions = width to height

        val left = (screenWidth - width) / 2
        val top = (screenHeight - height) / 2
        position.moveTo(left, top)
        renderableTopCorner = left to top

        if (waitingForInventoryUpdate && config.loadingText) {
            val loadingRenderable = Renderable.text(
                "§cLoading...",
                scale = activeScale / 100.0,
            )
            loadingPosition.moveTo(position.x + (width - loadingRenderable.width) / 2, position.y - loadingRenderable.height)
                .renderRenderable(loadingRenderable, posLabel = GUI_NAME, addToGuiManager = false)
        }

        DrawContextUtils.translatedPushPopResult(0f, 0f) {
            position.renderRenderable(renderable, posLabel = GUI_NAME, addToGuiManager = false)
            if (EstimatedItemValue.config.enabled) {
                DrawContextUtils.translate(0f, 0f)
                EstimatedItemValue.tryRendering()
            }
        }
    }

    private fun update() {
        displayRenderable = createRenderables()
    }

    private fun updateScreenSize(gui: Pair<Int, Int>): Boolean {
        val renderable = currentMaxSize ?: run {
            activeScale = config.spacing.globalScale.get()
            update()
            return true
        }
        val previousActiveScale = activeScale
        val unscaledRenderableWidth = renderable.first / activeScale.toDouble()
        val unscaledRenderableHeight = renderable.second / activeScale.toDouble()
        val autoScaleWidth = 0.95 * gui.first / unscaledRenderableWidth
        val autoScaleHeight = 0.95 * gui.second / unscaledRenderableHeight
        val maxScale = min(autoScaleWidth, autoScaleHeight).toInt()

        activeScale = config.spacing.globalScale.get().coerceAtMost(maxScale)

        return activeScale != previousActiveScale
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
        val containerWidth = (config.spacing.slotWidth.get() * (activeScale / 100.0)).toInt()
        val containerHeight = (config.spacing.slotHeight.get() * (activeScale / 100.0)).toInt()
        val playerWidth = (containerWidth * (config.spacing.playerScale.get() / 100.0))
        val horizontalSpacing = (config.spacing.horizontalSpacing.get() * (activeScale / 100.0)).toInt()
        val verticalSpacing = (config.spacing.verticalSpacing.get() * (activeScale / 100.0)).toInt()
        val backgroundPadding = (config.spacing.backgroundPadding.get() * (activeScale / 100.0)).toInt()
        val buttonVerticalSpacing = (config.spacing.buttonVerticalSpacing.get() * (activeScale / 100.0)).toInt()

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
                3.0 * (activeScale / 100.0),
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
                    tooltipKeybind = if (config.showTooltipOnlyKeybind) config.tooltipKeybind else GLFW.GLFW_KEY_UNKNOWN
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
                    armor = slot.armor,
                    inPage = slot.isInCurrentPage(),
                    playerWidth = playerWidth,
                    containerHeight = containerHeight,
                    containerWidth = containerWidth,
                    eyesFollowMouse = config.eyesFollowMouse
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

    private fun reset() {
        displayRenderable = null
    }

    private fun addButtons(): Renderable {
        val (horizontalSpacing, verticalSpacing) = with(config.spacing) {
            buttonHorizontalSpacing.get() * (activeScale / 100.0) to buttonVerticalSpacing.get() * (activeScale / 100.0)
        }

        val backButton = CustomWardrobe.createLabeledButton(
            "§aBack",
            onClick = {
                clickContainerSlot(48)
                reset()
                ArmorWardrobeApi.currentPage = null
            },
            scale = activeScale
        )
        val exitButton = CustomWardrobe.createLabeledButton(
            "§cClose",
            onClick = {
                clickContainerSlot(49)
                reset()
                ArmorWardrobeApi.currentPage = null
            },
            scale = activeScale
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
            scale = activeScale
        )

        val editButton = CustomWardrobe.createLabeledButton(
            "§bEdit",
            onClick = {
                DelayedRun.runNextTick {
                    reset()
                    CustomWardrobe.enterEditMode()
                }
            },
            scale = activeScale
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
        val textScale = 1.5 * (activeScale / 100.0)
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

    fun clickContainerSlot(slot: Int) {
        InventoryUtils.clickSlot(slot, windowId = menu.containerId)
        waitingForInventoryUpdate = true
    }

    fun addGuiBackground(renderable: Renderable, borderPadding: Int): Renderable =
        CustomRenderUtils.addGuiBackground(
            renderable,
            borderPadding,
            scale = activeScale,
            backgroundColor = config.color.backgroundColor
        ) {
            config::enabled.jumpToEditor()
            reset()
            ArmorWardrobeApi.currentPage = null
        }

    companion object {
        val config: CustomWardrobeConfig get() = CustomWardrobe.config

        private fun WardrobeSlot.getSlotColor(): Color = with(config.color) {
            when {
                isCurrentSlot() -> equippedColor
                favorite && !config.onlyFavorites -> favoriteColor
                else -> null
            }?.toColor()?.transformIf({ !isInCurrentPage() }) { darker() }
                ?: (if (isInCurrentPage()) samePageColor else otherPageColor).toColor()
                    .transformIf({ locked || isEmpty() }) { darker(0.2) }.addAlpha(100)
        }

        private fun WardrobeSlot.getOutlineColor(): Pair<Color, Color> {
            val (top, bottom) = config.color.topBorderColor.toColor() to config.color.bottomBorderColor.toColor()
            return when {
                isEmpty() || locked -> ColorUtils.TRANSPARENT_COLOR to ColorUtils.TRANSPARENT_COLOR
                !isInCurrentPage() -> top.darker(0.5) to bottom.darker(0.5)
                else -> top to bottom
            }
        }
    }
}
