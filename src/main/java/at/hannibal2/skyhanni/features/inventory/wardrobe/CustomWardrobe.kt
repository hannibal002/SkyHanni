package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.customwardrobe.CustomWardrobeConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniContainerOverlayScreen
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.utils.compat.SkyHanniOverlayTheme
import at.hannibal2.skyhanni.utils.compat.getTooltip
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedFakePlayerRenderable.Companion.animatedFakePlayer
import at.hannibal2.skyhanni.utils.renderables.animated.framed.EquipmentSlotAnimationState
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import java.awt.Color

class CustomWardrobeTheme(config: CustomWardrobeConfig) : SkyHanniOverlayTheme(
    backgroundColor = config.color.backgroundColor.toColor(),
    topBorderColor = config.color.topBorderColor.toColor(),
    bottomBorderColor = config.color.bottomBorderColor.toColor(),
    outlineThickness = config.spacing.outlineThickness.get(),
    outlineBlur = config.spacing.outlineBlur.get(),
    backgroundPadding = config.spacing.backgroundPadding.get(),
    borderRadius = 8,
) {
    val equippedColor: Color = config.color.equippedColor.toColor()
    val favoriteColor: Color = config.color.favoriteColor.toColor()
    val samePageColor: Color = config.color.samePageColor.toColor()
    val otherPageColor: Color = config.color.otherPageColor.toColor()
    val buttonTopBorder: Int = config.color.topBorderColor.toColor().rgb
    val buttonBottomBorder: Int = config.color.bottomBorderColor.toColor().rgb
}

@SkyHanniModule
object CustomWardrobe : SkyHanniContainerOverlayScreen(
    checkInventoryName = WardrobeApi.wardrobeDetector.checkInventoryName,
) {

    val config: CustomWardrobeConfig get() = SkyHanniMod.feature.inventory.customWardrobe

    private val inventoryButtonPosition: Position = Position().ignoreScale()
    private var inventoryButton: Renderable? = null
    var editMode = false
    private var waitingForInventoryUpdate = false

    private const val GUI_NAME = "Custom Wardrobe"

    override val theme: CustomWardrobeTheme get() = CustomWardrobeTheme(config)
    override val isPassthrough: Boolean get() = editMode
    override val shouldActivate: Boolean get() = config.enabled.get()
    override val configuredScale: Int get() = config.spacing.globalScale.get()
    override fun allowMouseClick(): Boolean = CustomWardrobeKeybinds.allowMouseClick()
    override fun allowKeyboardClick(): Boolean = CustomWardrobeKeybinds.allowKeyboardClick()

    // Edit button in normal wardrobe while in edit mode
    @HandleEvent(onlyOnSkyblock = true)
    fun onChestGuiRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!WardrobeApi.inWardrobe() || !config.enabled.get() || !editMode) return
        val gui = Minecraft.getInstance().screen as? SkyHanniGuiContainer ?: return
        val renderable = inventoryButton ?: addReEnableButton().also { inventoryButton = it }
        val posX = gui.leftPos + (1.05 * gui.imageWidth).toInt()
        val posY = gui.topPos + (gui.imageHeight - renderable.height) / 2
        inventoryButtonPosition.moveTo(posX, posY)
            .renderRenderable(renderable, posLabel = GUI_NAME, addToGuiManager = false)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryClose(event: InventoryCloseEvent) {
        waitingForInventoryUpdate = false
    }

    override fun onOverlayClose() {
        reset()
    }

    override fun onOverlayDrawScreen(mouseX: Int, mouseY: Int) {
        if (!waitingForInventoryUpdate || !config.loadingText.get()) return
        val renderable = displayRenderable ?: return
        val (left, top) = renderableTopCorner
        val loadingText = Renderable.text("§cLoading...", scale = activeScale / 100.0)
        val loadingLeft = left + (renderable.width - loadingText.width) / 2
        val loadingTop = top - loadingText.height
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(loadingLeft.toFloat(), loadingTop.toFloat())
            Renderable.withMousePosition(mouseX - loadingLeft, mouseY - loadingTop) {
                loadingText.render(0, 0)
            }
        }
    }

    override fun onAfterRender() {
        if (EstimatedItemValue.config.enabled) {
            DrawContextUtils.translate(0f, 0f)
            EstimatedItemValue.tryRendering()
        }
    }

    override fun onBrandingClick() {
        config::enabled.jumpToEditor()
        reset()
        WardrobeApi.currentPage = null
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        with(config.spacing) {
            ConditionalUtils.onToggle(
                globalScale, outlineThickness, outlineBlur,
                slotWidth, slotHeight, playerScale,
                maxPlayersPerRow, horizontalSpacing, verticalSpacing,
                buttonSlotsVerticalSpacing, buttonHorizontalSpacing, buttonVerticalSpacing,
                buttonWidth, buttonHeight, backgroundPadding,
            ) {
                invalidateScale()
                rebuildDisplay()
            }
        }
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!WardrobeApi.inWardrobe() || !config.enabled.get() || editMode) return
        DelayedRun.runNextTick {
            rebuildDisplay()
        }
    }

    private fun createWarning(list: List<WardrobeSlot>): Pair<String?, List<WardrobeSlot>> {
        var wardrobeWarning: String? = null
        var wardrobeSlots = list

        if (wardrobeSlots.isEmpty()) wardrobeWarning = "§cYour wardrobe is empty :("

        if (config.hideLockedSlots.get()) {
            wardrobeSlots = wardrobeSlots.filter { !it.locked }
            if (wardrobeSlots.isEmpty()) wardrobeWarning = "§cAll your slots are locked? Somehow"
        }

        if (config.hideEmptySlots.get()) {
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
        containerWidth: Int,
    ): Renderable {
        val loreList = mutableListOf<Renderable>()
        val height = containerHeight - 3

        // This is needed to keep the background size the same as the player renderable size
        val hoverableSizes = MutableList(4) { height / 4 }.apply {
            for (k in 0 until height % 4) this[k]++
        }

        for (armorIndex in 0 until 4) {
            val stack = slot.armor.getOrNull(armorIndex)?.copy()
            var renderable = Renderable.placeholder(containerWidth, hoverableSizes[armorIndex])
            if (stack != null) {
                renderable = Renderable.hoverTips(
                    renderable,
                    tipsProvider = { getToolTip(stack, slot).orEmpty() },
                    stack = stack,
                    condition = {
                        !config.showTooltipOnlyKeybind.get() || config.tooltipKeybind.isKeyHeld()
                    },
                    onHover = {
                        if (EstimatedItemValue.config.enabled) EstimatedItemValue.updateItem(stack)
                    },
                )
            }
            loreList.add(renderable)
        }
        return Renderable.vertical(loreList, spacing = 1)
    }

    private fun getToolTip(stack: ItemStack, slot: WardrobeSlot): List<Component>? {
        try {
            return stack.getTooltip(Minecraft.getInstance().options.advancedItemTooltips)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to get tooltip for armor piece in CustomWardrobe",
                "Armor" to stack,
                "Slot" to slot,
                "Lore" to stack.getTooltipCompat(false),
            )
            return null
        }
    }

    private fun createFakePlayerRenderable(
        slot: WardrobeSlot,
        playerWidth: Double,
        containerHeight: Int,
        containerWidth: Int,
    ): Renderable {
        val fakePlayer = FakePlayer()
        var scale = playerWidth
        val animatedSlots = mutableMapOf<EquipmentSlot, EquipmentSlotAnimationState>()

        for (equipment in Inventory.EQUIPMENT_SLOT_MAPPING.values) {
            val armorOrdinal = equipment.ordinal - 2
            if (armorOrdinal !in 0..3) continue
            val raw = slot.armor.reversed()[armorOrdinal]?.copy()?.removeEnchants() ?: ItemStack.EMPTY
            val frames = if (config.animatedSkins.get()) {
                WardrobeApi.getArmorAnimatedFrames(raw) ?: WardrobeApi.getArmorDyeAnimatedFrames(raw)
            } else null
            fakePlayer.equipment.set(equipment, frames?.firstOrNull()?.stack ?: raw)
            if (frames != null && frames.size > 1) {
                animatedSlots[equipment] = EquipmentSlotAnimationState(frames)
            }
        }

        val playerColor = if (!slot.isInCurrentPage()) {
            scale *= 0.9
            Color.GRAY.addAlpha(100)
        } else null

        return Renderable.animatedFakePlayer(
            player = fakePlayer,
            animatedSlots = animatedSlots,
            followMouse = config.eyesFollowMouse,
            width = containerWidth,
            height = containerHeight,
            entityScale = scale.toInt(),
            padding = 0,
            color = playerColor,
        )
    }

    override fun buildContent(): Renderable {
        val (wardrobeWarning, list) = createWarning(WardrobeApi.slots)

        val maxPlayersPerRow = config.spacing.maxPlayersPerRow.get().coerceAtLeast(1)
        val containerWidth = (config.spacing.slotWidth.get() * (activeScale / 100.0)).toInt()
        val containerHeight = (config.spacing.slotHeight.get() * (activeScale / 100.0)).toInt()
        val playerWidth = (containerWidth * (config.spacing.playerScale.get() / 100.0))
        val horizontalSpacing = (config.spacing.horizontalSpacing.get() * (activeScale / 100.0)).toInt()
        val verticalSpacing = (config.spacing.verticalSpacing.get() * (activeScale / 100.0)).toInt()
        val buttonVerticalSpacing = (config.spacing.buttonVerticalSpacing.get() * (activeScale / 100.0)).toInt()

        var maxRenderableWidth = maxPlayersPerRow * containerWidth + (maxPlayersPerRow - 1) * horizontalSpacing

        val button = addButtons()

        if (button.width > maxRenderableWidth) maxRenderableWidth = button.width

        wardrobeWarning?.let { text ->
            val warningRenderable = Renderable.wrappedText(
                text,
                maxRenderableWidth,
                3.0 * (activeScale / 100.0),
                horizontalAlign = HorizontalAlignment.CENTER,
            )
            return Renderable.vertical(
                warningRenderable,
                button,
                spacing = buttonVerticalSpacing,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
        }

        val chunkedList = list.chunked(maxPlayersPerRow)

        val rowsRenderables = chunkedList.map { row ->
            val slotsRenderables = row.map { slot ->
                val armorTooltipRenderable = createArmorTooltipRenderable(slot, containerHeight, containerWidth)
                val (topOutline, bottomOutline) = slot.getOutlineColor()

                val playerBackground = createHoverableRenderable(
                    armorTooltipRenderable,
                    topLayerRenderable = addSlotHoverableButtons(slot),
                    hoveredColor = slot.getSlotColor(),
                    borderOutlineThickness = config.spacing.outlineThickness.get(),
                    borderOutlineBlur = config.spacing.outlineBlur.get(),
                    onClick = { slot.clickSlot() },
                    topOutlineColor = topOutline,
                    bottomOutlineColor = bottomOutline,
                )

                val playerRenderable = createFakePlayerRenderable(slot, playerWidth, containerHeight, containerWidth)

                Renderable.doubleLayered(playerBackground, playerRenderable, blockBottomHover = false, forceBottomRenderFirst = true)
            }
            Renderable.horizontal(slotsRenderables, horizontalSpacing)
        }

        val allSlotsRenderable = Renderable.vertical(
            rowsRenderables,
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        return Renderable.vertical(
            listOf(allSlotsRenderable, button),
            buttonVerticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        )
    }

    private fun reset() {
        editMode = false
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
                WardrobeApi.currentPage = null
            },
        )
        val exitButton = createLabeledButton(
            "§cClose",
            onClick = {
                InventoryUtils.clickSlot(49)
                reset()
                WardrobeApi.currentPage = null
            },
        )

        val greenColor = Color(85, 255, 85, 200)
        val redColor = Color(255, 85, 85, 200)

        val onlyFavoriteButton = createLabeledButton(
            "§eFavorite",
            hoveredColor = if (config.onlyFavorites) greenColor else redColor,
            onClick = {
                config.onlyFavorites = !config.onlyFavorites
                rebuildDisplay()
            },
        )

        val editButton = createLabeledButton(
            "§bEdit",
            onClick = {
                DelayedRun.runNextTick {
                    reset()
                    editMode = true
                }
            },
        )

        val row = Renderable.horizontal(
            backButton,
            exitButton,
            onlyFavoriteButton,
            spacing = horizontalSpacing.toInt(),
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        return Renderable.vertical(
            row,
            editButton,
            spacing = verticalSpacing.toInt(),
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        )
    }

    private fun addReEnableButton(): Renderable {
        val color = Color(116, 150, 255, 200)
        return createLabeledButton(
            "§bEdit",
            hoveredColor = color,
            unhoveredColor = color.darker(0.8),
            onClick = {
                editMode = false
                rebuildDisplay()
            },
        )
    }

    private fun addSlotHoverableButtons(wardrobeSlot: WardrobeSlot): Renderable {
        val textScale = 1.5 * (activeScale / 100.0)
        val shouldRender = !wardrobeSlot.isEmpty() && !wardrobeSlot.locked
        if (!shouldRender && !wardrobeSlot.favorite) return Renderable.placeholder(0, 0)
        val list = buildList {
            add(
                Renderable.clickable(
                    Renderable.hoverable(
                        centerString((if (wardrobeSlot.favorite) "§c" else "§7") + "❤", scale = textScale),
                        centerString((if (wardrobeSlot.favorite) "§4" else "§8") + "❤", scale = textScale),
                    ),
                    onLeftClick = {
                        wardrobeSlot.favorite = !wardrobeSlot.favorite
                        rebuildDisplay()
                    },
                ),
            )
            if (config.estimatedValue.get() && shouldRender) {
                add(
                    Renderable.hoverTips(
                        centerString("§2$", scale = textScale),
                        WardrobeApi.createPriceLore(wardrobeSlot),
                    ),
                )
            }
        }

        return Renderable.vertical(list, 1, HorizontalAlignment.RIGHT)
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

        val t = theme
        return Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        Renderable.placeholder(buttonWidth, buttonHeight),
                        onClick,
                    ),
                    centerString(text, scale = textScale),
                    false,
                ),
                hoveredColor,
                padding = 0,
                topOutlineColor = t.buttonTopBorder,
                bottomOutlineColor = t.buttonBottomBorder,
                borderOutlineThickness = 2,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.doubleLayered(
                    Renderable.placeholder(buttonWidth, buttonHeight),
                    centerString(text, scale = textScale),
                ),
                unhoveredColor.darker(0.57),
                padding = 0,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
        )
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
        topOutlineColor: Color,
        bottomOutlineColor: Color,
    ): Renderable =
        Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        hoveredRenderable,
                        onClick,
                    ),
                    topLayerRenderable,
                ),
                hoveredColor,
                padding = padding,
                topOutlineColor = topOutlineColor.rgb,
                bottomOutlineColor = bottomOutlineColor.rgb,
                borderOutlineThickness = borderOutlineThickness,
                blur = borderOutlineBlur,
                horizontalAlign = horizontalAlignment,
                verticalAlign = verticalAlignment,
            ),
            Renderable.drawInsideRoundedRect(
                unhoveredRenderable,
                unHoveredColor,
                padding = padding,
                horizontalAlign = horizontalAlignment,
                verticalAlign = verticalAlignment,
            ),
            onHover = { onHover() },
        )

    private fun WardrobeSlot.getOutlineColor(): Pair<Color, Color> {
        val t = theme
        val top = t.topBorderColor
        val bottom = t.bottomBorderColor
        return when {
            isEmpty() || locked -> ColorUtils.TRANSPARENT_COLOR to ColorUtils.TRANSPARENT_COLOR
            !isInCurrentPage() -> top.darker(0.5) to bottom.darker(0.5)
            else -> top to bottom
        }
    }

    fun WardrobeSlot.clickSlot() {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = WardrobeApi.currentPage ?: return
        if (isInCurrentPage()) {
            if (isEmpty() || locked || waitingForInventoryUpdate) return
            WardrobeApi.currentSlot = if (isCurrentSlot()) null else id
            InventoryUtils.clickSlot(inventorySlot)
        } else {
            if (page < wardrobePage) {
                WardrobeApi.currentPage = wardrobePage - 1
                waitingForInventoryUpdate = true
                InventoryUtils.clickSlot(previousPageSlot)
            } else if (page > wardrobePage) {
                WardrobeApi.currentPage = wardrobePage + 1
                waitingForInventoryUpdate = true
                InventoryUtils.clickSlot(nextPageSlot)
            }
        }
        rebuildDisplay()
    }

    private fun WardrobeSlot.getSlotColor(): Color {
        val t = theme
        return with(t) {
            when {
                isCurrentSlot() -> equippedColor
                favorite && !config.onlyFavorites -> favoriteColor
                else -> null
            }?.transformIf({ !isInCurrentPage() }) { darker() }
                ?: (if (isInCurrentPage()) samePageColor else otherPageColor)
                    .transformIf({ locked || isEmpty() }) { darker(0.2) }.addAlpha(100)
        }
    }

    private fun centerString(
        text: String,
        scale: Double = 1.0,
        color: Color = Color.WHITE,
    ) = Renderable.text(text, scale, color, horizontalAlign = HorizontalAlignment.CENTER)

    @JvmStatic
    fun shouldHideNormalTooltip(): Boolean = isOverlayVisible
}
