package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.MAX_PAGES
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.MAX_SLOT_PER_PAGE
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.getFakePlayer
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CustomWardrobe {

    private val config get() = SkyHanniMod.feature.inventory.customWardrobe

    private var displayRenderable: Renderable? = null
    private var inventoryButton: Renderable? = null
    private var editMode = false
    private var waitingForInventoryUpdate = false

    private var activeScale: Int = 100
    private var currentMaxSize: Pair<Int, Int>? = null
    private var lastScreenSize: Pair<Int, Int>? = null
    private var guiName = "Custom Wardrobe"

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled() || editMode) return
        val renderable = displayRenderable ?: run {
            update()
            displayRenderable ?: return
        }

        val gui = event.gui
        val screenSize = gui.width to gui.height

        if (screenSize != lastScreenSize) {
            lastScreenSize = screenSize
            val didUpdate = updateScreenSize(screenSize)
            if (didUpdate) return
        }

        val (width, height) = renderable.width to renderable.height
        val pos = Position((gui.width - width) / 2, (gui.height - height) / 2)
        if (waitingForInventoryUpdate && config.loadingText) {
            val loadingRenderable = Renderable.string(
                "§cLoading...",
                scale = activeScale / 100.0
            )
            val loadingPos =
                Position(pos.rawX + (width - loadingRenderable.width) / 2, pos.rawY - loadingRenderable.height)
            loadingPos.renderRenderable(loadingRenderable, posLabel = guiName, addToGuiManager = false)
        }

        GlStateManager.translate(0f, 0f, 100f)
        pos.renderRenderable(renderable, posLabel = guiName, addToGuiManager = false)
        GlStateManager.translate(0f, 0f, -100f)
        event.cancel()
    }

    // Edit button in normal wardrobe while in edit mode
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!editMode) return
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return
        val renderable = inventoryButton ?: addReEnableButton().also { inventoryButton = it }
        val accessorGui = gui as AccessorGuiContainer
        val posX = accessorGui.guiLeft + (1.05 * accessorGui.width).toInt()
        val posY = accessorGui.guiTop + (accessorGui.height - renderable.height) / 2
        Position(posX, posY).renderRenderable(renderable, posLabel = guiName, addToGuiManager = false)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        waitingForInventoryUpdate = false
        if (!isEnabled()) return
        DelayedRun.runDelayed(500.milliseconds) {
            if (!WardrobeAPI.inWardrobe()) {
                reset()
            }
        }
    }

    @SubscribeEvent
    fun onConfigUpdate(event: ConfigLoadEvent) {
        with(config.spacing) {
            ConditionalUtils.onToggle(
                globalScale, outlineThickness, outlineBlur,
                slotWidth, slotHeight, playerScale,
                maxPlayersPerRow, horizontalSpacing, verticalSpacing,
                buttonSlotsVerticalSpacing, buttonHorizontalSpacing, buttonVerticalSpacing,
                buttonWidth, buttonHeight, backgroundPadding,
            ) {
                currentMaxSize = null
                lastScreenSize = null
            }
        }
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled() || editMode) return
        update()
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
        val unscaledRenderableWidth = renderable.first / activeScale
        val unscaledRenderableHeight = renderable.second / activeScale
        val autoScaleWidth = 0.95 * gui.first / unscaledRenderableWidth
        val autoScaleHeight = 0.95 * gui.second / unscaledRenderableHeight
        val maxScale = min(autoScaleWidth, autoScaleHeight).toInt()

        activeScale = config.spacing.globalScale.get().coerceAtMost(maxScale)
        return if (activeScale != previousActiveScale) {
            update()
            true
        } else false
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

    private fun createArmorTooltipRenderable(
        slot: WardrobeSlot,
        containerHeight: Int,
        containerWidth: Int
    ): Renderable {
        val loreList = mutableListOf<Renderable>()
        val height = containerHeight - 3

        // This is needed to keep the background size the same as the player renderable size
        val hoverableSizes = MutableList(4) { height / 4 }.apply {
            for (k in 0 until height % 4) this[k]++
        }

        for (armorIndex in 0 until 4) {
            val stack = slot.armor[armorIndex]?.copy()
            if (stack == null) {
                loreList.add(Renderable.placeholder(containerWidth, hoverableSizes[armorIndex]))
            } else {
                loreList.add(
                    Renderable.hoverable(
                        Renderable.hoverTips(
                            Renderable.placeholder(containerWidth, hoverableSizes[armorIndex]),
                            stack.getTooltip(Minecraft.getMinecraft().thePlayer, false)
                        ),
                        Renderable.placeholder(containerWidth, hoverableSizes[armorIndex]),
                        bypassChecks = true
                    )
                )
            }
        }
        return Renderable.verticalContainer(loreList, spacing = 1)
    }

    private fun createFakePlayerRenderable(
        slot: WardrobeSlot,
        playerWidth: Double,
        containerHeight: Int,
        containerWidth: Int
    ): Renderable {
        val fakePlayer = getFakePlayer()
        var scale = playerWidth

        fakePlayer.inventory.armorInventory =
            slot.armor.map { it?.copy()?.removeEnchants() }.reversed().toTypedArray()

        val playerColor = if (!slot.isInCurrentPage()) {
            scale *= 0.9
            Color.GRAY.withAlpha(100)
        } else null

        return Renderable.fakePlayer(
            fakePlayer,
            followMouse = config.eyesFollowMouse,
            width = containerWidth,
            height = containerHeight,
            entityScale = scale.toInt(),
            padding = 0,
            color = playerColor,
        )
    }

    private fun createRenderables(): Renderable {
        val (wardrobeWarning, list) = createWarning(WardrobeAPI.slots)

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
            val warningRenderable = Renderable.wrappedString(
                text,
                maxRenderableWidth,
                3.0 * (activeScale / 100.0),
                horizontalAlign = HorizontalAlignment.CENTER
            )
            val withButtons = Renderable.verticalContainer(
                listOf(warningRenderable, button),
                buttonVerticalSpacing,
                horizontalAlign = HorizontalAlignment.CENTER
            )
            return addGuiBackground(withButtons, backgroundPadding)
        }

        val chunkedList = list.chunked(maxPlayersPerRow)

        val rowsRenderables = chunkedList.map { row ->
            val slotsRenderables = row.map { slot ->
                val armorTooltipRenderable = createArmorTooltipRenderable(slot, containerHeight, containerWidth)

                val playerBackground = createHoverableRenderable(
                    armorTooltipRenderable,
                    topLayerRenderable = addSlotHoverableButtons(slot),
                    hoveredColor = slot.getSlotColor(),
                    borderOutlineThickness = config.spacing.outlineThickness.get(),
                    borderOutlineBlur = config.spacing.outlineBlur.get(),
                    onClick = { slot.clickSlot() }
                )

                val playerRenderable = createFakePlayerRenderable(slot, playerWidth, containerHeight, containerWidth)

                Renderable.doubleLayered(playerBackground, playerRenderable, false)
            }
            Renderable.horizontalContainer(slotsRenderables, horizontalSpacing)
        }

        val allSlotsRenderable = Renderable.verticalContainer(
            rowsRenderables,
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER
        )

        val withButtons = Renderable.verticalContainer(
            listOf(allSlotsRenderable, button),
            buttonVerticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER
        )

        return addGuiBackground(withButtons, backgroundPadding)
    }

    private fun addGuiBackground(renderable: Renderable, borderPadding: Int) =
        Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                renderable,
                Renderable.clickable(
                    Renderable.string(
                        "§7SkyHanni",
                        horizontalAlign = HorizontalAlignment.RIGHT,
                        verticalAlign = VerticalAlignment.BOTTOM,
                        scale = 1.0 * (activeScale / 100.0)
                    ).let { Renderable.hoverable(hovered = Renderable.underlined(it), unhovered = it) },
                    onClick = {
                        config::enabled.jumpToEditor()
                        reset()
                        WardrobeAPI.currentPage = null
                    }
                ),
                blockBottomHover = false
            ),
            config.color.backgroundColor.toChromaColor(),
            padding = borderPadding
        )

    private fun reset() {
        WardrobeAPI.inCustomWardrobe = false
        editMode = false
        displayRenderable = null
        inventoryButton = null
    }

    private fun addButtons(): Renderable {
        val (horizontalSpacing, verticalSpacing) = with(config.spacing) {
            buttonHorizontalSpacing.get() * (activeScale / 100.0) to buttonVerticalSpacing.get() * (activeScale / 100.0)
        }

        val backButton = createLabeledButton(
            "§aBack",
            onClick = {
                InventoryUtils.clickSlot(48)
                reset()
                WardrobeAPI.currentPage = null
            }
        )
        val exitButton = createLabeledButton(
            "§cClose",
            onClick = {
                InventoryUtils.clickSlot(49)
                reset()
                WardrobeAPI.currentPage = null
            }
        )

        val greenColor = Color(85, 255, 85, 200)
        val redColor = Color(255, 85, 85, 200)

        val onlyFavoriteButton = createLabeledButton(
            "§eFavorite",
            hoveredColor = if (config.onlyFavorites) greenColor else redColor,
            onClick = {
                config.onlyFavorites = !config.onlyFavorites
                update()
            }
        )

        val editButton = createLabeledButton(
            "§bEdit",
            onClick = {
                DelayedRun.runNextTick {
                    reset()
                    editMode = true
                }
            }
        )

        val row = Renderable.horizontalContainer(
            listOf(backButton, exitButton, onlyFavoriteButton),
            horizontalSpacing.toInt(),
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        val total = Renderable.verticalContainer(
            listOf(row, editButton),
            verticalSpacing.toInt(),
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER
        )

        return total
    }

    private fun addReEnableButton(): Renderable {
        val color = Color(116, 150, 255, 200)
        return createLabeledButton(
            "§bEdit",
            hoveredColor = color,
            unhoveredColor = color.darker(0.8),
            onClick = {
                WardrobeAPI.inCustomWardrobe = false
                editMode = false
                update()
            }
        )
    }

    private fun addSlotHoverableButtons(wardrobeSlot: WardrobeSlot): Renderable {
        val list = mutableListOf<Renderable>()
        val textScale = 1.5 * (activeScale / 100.0)
        list.add(
            Renderable.clickable(
                Renderable.hoverable(
                    Renderable.string(
                        (if (wardrobeSlot.favorite) "§c" else "§7") + "❤",
                        scale = textScale,
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.CENTER
                    ),
                    Renderable.string(
                        (if (wardrobeSlot.favorite) "§4" else "§8") + "❤",
                        scale = textScale,
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.CENTER
                    )
                ),
                onClick = {
                    wardrobeSlot.favorite = !wardrobeSlot.favorite
                    update()
                }
            )
        )

        if (config.estimatedValue && !wardrobeSlot.isEmpty()) {
            val lore = WardrobeAPI.createPriceLore(wardrobeSlot)
            list.add(
                Renderable.hoverTips(
                    Renderable.string(
                        "§2$",
                        scale = textScale,
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.CENTER
                    ), lore
                )
            )
        }

        return Renderable.verticalContainer(list, 1, HorizontalAlignment.RIGHT)
    }

    private fun createLabeledButton(
        text: String,
        hoveredColor: Color = Color(130, 130, 130, 200),
        unhoveredColor: Color = hoveredColor.darker(0.57),
        onClick: () -> Unit,
    ): Renderable {
        val buttonWidth = (config.spacing.buttonWidth.get() * (activeScale / 100.0)).toInt()
        val buttonHeight = (config.spacing.buttonHeight.get() * (activeScale / 100.0)).toInt()
        val textScale = (activeScale / 100.0)

        val renderable = Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        Renderable.placeholder(buttonWidth, buttonHeight),
                        onClick
                    ),
                    Renderable.string(
                        text,
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.CENTER,
                        scale = textScale
                    ),
                    false,
                ),
                hoveredColor,
                padding = 0,
                topOutlineColor = config.color.topBorderColor.toChromaColorInt(),
                bottomOutlineColor = config.color.bottomBorderColor.toChromaColorInt(),
                borderOutlineThickness = 2,
                horizontalAlign = HorizontalAlignment.CENTER
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.doubleLayered(
                    Renderable.placeholder(buttonWidth, buttonHeight),
                    Renderable.string(
                        text,
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.CENTER,
                        scale = textScale
                    ),
                ),
                unhoveredColor.darker(0.57),
                padding = 0,
                horizontalAlign = HorizontalAlignment.CENTER
            )
        )

        return renderable
    }

    private fun createHoverableRenderable(
        hoveredRenderable: Renderable,
        unhoveredRenderable: Renderable = Renderable.placeholder(hoveredRenderable.width, hoveredRenderable.height),
        topLayerRenderable: Renderable = Renderable.placeholder(0, 0),
        padding: Int = 0,
        horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
        verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
        hoveredColor: Color,
        unHoveredColor: Color = hoveredColor,
        borderOutlineThickness: Int,
        borderOutlineBlur: Float = 0.5f,
        onClick: () -> Unit,
        onHover: () -> Unit = {},
    ): Renderable =
        Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        hoveredRenderable,
                        onClick
                    ), topLayerRenderable
                ),
                hoveredColor,
                padding = padding,
                topOutlineColor = config.color.topBorderColor.toChromaColorInt(),
                bottomOutlineColor = config.color.bottomBorderColor.toChromaColorInt(),
                borderOutlineThickness = borderOutlineThickness,
                blur = borderOutlineBlur,
                horizontalAlign = horizontalAlignment,
                verticalAlign = verticalAlignment
            ),
            Renderable.drawInsideRoundedRect(
                unhoveredRenderable,
                unHoveredColor,
                padding = padding,
                horizontalAlign = horizontalAlignment,
                verticalAlign = verticalAlignment
            ),
            onHover = { onHover() }
        )

    private fun WardrobeSlot.clickSlot() {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = WardrobeAPI.currentPage ?: return
        if (isInCurrentPage()) {
            if (isEmpty() || locked || waitingForInventoryUpdate) return
            WardrobeAPI.currentSlot = if (isCurrentSlot()) null else id
            InventoryUtils.clickSlot(inventorySlot)
        } else {
            if (page < wardrobePage) {
                WardrobeAPI.currentPage = wardrobePage - 1
                waitingForInventoryUpdate = true
                InventoryUtils.clickSlot(previousPageSlot)
            } else if (page > wardrobePage) {
                WardrobeAPI.currentPage = wardrobePage + 1
                waitingForInventoryUpdate = true
                InventoryUtils.clickSlot(nextPageSlot)
            }
        }
        update()
    }

    private fun WardrobeSlot.getSlotColor(): Color = with(config.color) {
        when {
            isCurrentSlot() -> equippedColor
            favorite -> favoriteColor
            else -> null
        }?.toChromaColor()?.transformIf({ isInCurrentPage() }) { darker() }
            ?: (if (isInCurrentPage()) samePageColor else otherPageColor).toChromaColor()
                .transformIf({ locked || isEmpty() }) { darker(0.2) }.addAlpha(100)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && WardrobeAPI.inWardrobe()
}
