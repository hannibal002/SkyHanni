package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.createWardrobePriceLore
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.currentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.currentWardrobeSlot
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inCustomWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inWardrobe
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.getFakePlayer
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CustomWardrobe {

    private val config get() = SkyHanniMod.feature.inventory.customWardrobe

    private var displayRenderable: Renderable? = null
    private var inventoryButton: Renderable? = null
    private var editMode = false
    private var waitingForInventoryUpdate = false
    private var lastEditClick = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled() || editMode) return
        val gui = event.gui
        val renderable = displayRenderable ?: run {
            update()
            displayRenderable ?: return
        }

        // Change global wardrobe scale if its taller or wider than the screen
        if (renderable.width > gui.width || renderable.height > gui.height) {
            if (config.spacing.globalScale <= 1) return
            val newScale = (config.spacing.globalScale * (0.9 / max(
                renderable.width.toDouble() / gui.width,
                renderable.height.toDouble() / gui.height
            ))).toInt()
            config.spacing.globalScale = newScale
            ChatUtils.clickableChat(
                "Auto-set your Global Scale in custom wardrobe, as it was too tall/wide.",
                onClick = { config::spacing.jumpToEditor() }
            )
            update()
        }

        val (width, height) = renderable.width to renderable.height
        val pos = Position((gui.width - width) / 2, (gui.height - height) / 2)
        if (waitingForInventoryUpdate && config.loadingText) {
            val loadingRenderable = Renderable.string("§cLoading...")
            val loadingPos =
                Position(pos.rawX + (width - loadingRenderable.width) / 2, pos.rawY - loadingRenderable.height)
            loadingPos.renderRenderable(loadingRenderable, posLabel = "Custom Wardrobe", addToGuiManager = false)
        }

        GlStateManager.translate(0f, 0f, 100f)
        pos.renderRenderable(renderable, posLabel = "Custom Wardrobe", addToGuiManager = false)
        GlStateManager.translate(0f, 0f, -100f)
        event.cancel()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!editMode) return
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return
        val renderable = inventoryButton ?: addReEnableButton().also { inventoryButton = it }
        val accessorGui = gui as AccessorGuiContainer
        val posX = accessorGui.guiLeft + (1.05 * accessorGui.width).toInt()
        val posY = accessorGui.guiTop + (accessorGui.height - renderable.height) / 2
        Position(posX, posY).renderRenderable(renderable, posLabel = "Custom Wardrobe", addToGuiManager = false)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        waitingForInventoryUpdate = false
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) {
                reset()
            }
        }
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled() || editMode) return
        update()
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (lastEditClick.passedSince() < 50.milliseconds) event.cancel()
    }

    private fun update() {
        displayRenderable = createRenderables()
    }

    private fun createRenderables(): Renderable {
        var list = WardrobeAPI.wardrobeSlots
        var wardrobeWarning: String? = null

        if (list.isEmpty()) wardrobeWarning = "§cYour wardrobe is empty :("

        if (config.hideLockedSlots) {
            list = list.filter { !it.locked }
            if (list.isEmpty()) wardrobeWarning = "§cAll your slots are locked? Somehow"
        }

        if (config.hideEmptySlots) {
            list = list.filter { !it.isEmpty() }
            if (list.isEmpty()) wardrobeWarning = "§cAll slots are empty :("
        }
        if (config.onlyFavorites) {
            list = list.filter { it.favorite || it.isCurrentSlot() }
            if (list.isEmpty()) wardrobeWarning = "§cDidn't set any favorites"
        }

        wardrobeWarning?.let { text ->
            val warningRenderable = Renderable.string(
                text,
                3.0 * (config.spacing.globalScale / 100.0),
                horizontalAlign = HorizontalAlignment.CENTER
            )
            return warningRenderable
        }

        val maxPlayersPerRow = config.spacing.maxPlayersPerRow
        val containerWidth = (config.spacing.slotWidth * (config.spacing.globalScale / 100.0)).toInt()
        val containerHeight = (config.spacing.slotHeight * (config.spacing.globalScale / 100.0)).toInt()
        val playerWidth = (containerWidth * config.spacing.playerScale) / 100.0
        val horizontalSpacing = (config.spacing.horizontalSpacing * (config.spacing.globalScale / 100.0)).toInt()
        val verticalSpacing = (config.spacing.verticalSpacing * (config.spacing.globalScale / 100.0)).toInt()

        val chunkedList = list.chunked(maxPlayersPerRow)

        val rowsRenderables = chunkedList.map { row ->
            val slotsRenderables = row.map { slot ->
                var scale = playerWidth
                val armorTooltipRenderable = {
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
                    Renderable.verticalContainer(loreList, spacing = 1)
                }

                val playerBackground = createHoverableRenderable(
                    armorTooltipRenderable.invoke(),
                    topLayerRenderable = addSlotHoverableButtons(slot),
                    hoveredColor = slot.getSlotColor(),
                    borderOutlineThickness = config.spacing.outlineThickness,
                    borderOutlineBlur = config.spacing.outlineBlur,
                    onClick = { slot.clickSlot() }
                )

                val fakePlayer = getFakePlayer()

                fakePlayer.inventory.armorInventory =
                    slot.armor.map { it?.copy()?.removeEnchants() }.reversed().toTypedArray()

                val playerColor = if (!slot.isInCurrentPage()) {
                    scale *= 0.9
                    Color.GRAY.withAlpha(100)
                } else null

                val playerRenderable = Renderable.fakePlayer(
                    fakePlayer,
                    followMouse = config.eyesFollowMouse,
                    width = containerWidth,
                    height = containerHeight,
                    entityScale = scale.toInt(),
                    padding = 0,
                    color = playerColor,
                )

                Renderable.doubleLayered(playerBackground, playerRenderable, false)
            }
            Renderable.horizontalContainer(slotsRenderables, horizontalSpacing)
        }

        val allSlotsRenderable = Renderable.verticalContainer(
            rowsRenderables,
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER
        )

        val button = addButtons()

        val fullRenderable = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.verticalContainer(
                    listOf(allSlotsRenderable, button),
                    config.spacing.buttonSlotsVerticalSpacing,
                    horizontalAlign = HorizontalAlignment.CENTER
                ),
                Renderable.clickable(
                    Renderable.string(
                        "§7SkyHanni",
                        horizontalAlign = HorizontalAlignment.RIGHT,
                        verticalAlign = VerticalAlignment.BOTTOM,
                        scale = 1.0 * (config.spacing.globalScale / 100.0)
                    ).let { Renderable.hoverable(hovered = Renderable.underlined(it), unhovered = it) },
                    onClick = {
                        config::enabled.jumpToEditor()
                        reset()
                        currentPage = null
                    }
                ),
                blockBottomHover = false
            ),
            config.color.backgroundColor.toChromaColor(),
            padding = 10
        )
        return fullRenderable
    }

    private fun reset() {
        inCustomWardrobe = false
        editMode = false
        displayRenderable = null
        inventoryButton = null
    }

    private fun addButtons(): Renderable {
        val (horizontalSpacing, verticalSpacing) = with(config.spacing) {
            buttonHorizontalSpacing * (globalScale / 100.0) to buttonVerticalSpacing * (globalScale / 100.0)
        }

        val backButton = createLabeledButton(
            "§aBack",
            onClick = {
                clickSlot(48, getWindowId() ?: -1)
                reset()
                currentPage = null
            }
        )
        val exitButton = createLabeledButton(
            "§cClose",
            onClick = {
                clickSlot(49, getWindowId() ?: -1)
                reset()
                currentPage = null
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
                reset()
                lastEditClick = SimpleTimeMark.now()
                editMode = true
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
                inCustomWardrobe = false
                editMode = false
                lastEditClick = SimpleTimeMark.now()
                update()
            }
        )
    }

    private fun addSlotHoverableButtons(wardrobeSlot: WardrobeAPI.WardrobeSlot): Renderable {
        val list = mutableListOf<Renderable>()
        val textScale = 1.5 * (config.spacing.globalScale / 100.0)
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
            val lore = createWardrobePriceLore(wardrobeSlot)
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
        onClick: () -> Unit
    ): Renderable {
        val buttonWidth = (config.spacing.buttonWidth * (config.spacing.globalScale / 100.0)).toInt()
        val buttonHeight = (config.spacing.buttonHeight * (config.spacing.globalScale / 100.0)).toInt()
        val textScale = 1.0 * (config.spacing.globalScale / 100.0)

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

    private fun WardrobeAPI.WardrobeSlot.clickSlot() {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = currentPage ?: return
        val windowId = getWindowId() ?: -1
        if (isInCurrentPage()) {
            if (isEmpty() || locked || waitingForInventoryUpdate) return
            currentWardrobeSlot = if (isCurrentSlot()) null
            else id
            clickSlot(inventorySlot, windowId)
        } else {
            if (page < wardrobePage) {
                currentPage = wardrobePage - 1
                waitingForInventoryUpdate = true
                clickSlot(previousPageSlot, windowId)
            } else if (page > wardrobePage) {
                currentPage = wardrobePage + 1
                waitingForInventoryUpdate = true
                clickSlot(nextPageSlot, windowId)
            }
        }
        update()
    }

    private fun WardrobeAPI.WardrobeSlot.getSlotColor(): Color {
        with(config.color) {
            return Color((when {
                isCurrentSlot() -> equippedColor
                locked || isEmpty() -> lockedEmptyColor
                favorite -> favoriteColor
                else -> null
            }?.toChromaColor()?.let { if (!isInCurrentPage()) it.darker() else it }
                ?: (if (!isInCurrentPage()) samePageColor else otherPageColor).toChromaColor()
                ).withAlpha(100), true)
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && inWardrobe() && config.enabled
}
