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
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.favorite
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.getArmor
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inCustomWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isCurrentSlot
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isEmpty
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isInCurrentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.locked
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

class CustomWardrobe {

    private val config get() = SkyHanniMod.feature.inventory.customWardrobe

    private var displayRenderable: Renderable? = null
    private var buttonsRenderable: Renderable? = null
    private var editMode = false

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        event.cancel()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        if (displayRenderable == null) {
            update()
        }
        val renderable = displayRenderable ?: return
        val button = buttonsRenderable ?: return

        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return

        val fullRenderable = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.verticalContainer(
                    listOf(renderable, button),
                    config.spacing.buttonSlotsVerticalSpacing,
                    horizontalAlign = HorizontalAlignment.CENTER
                ),
                Renderable.clickable(
                    Renderable.string(
                        "§7SkyHanni",
                        horizontalAlign = HorizontalAlignment.RIGHT,
                        verticalAlign = VerticalAlignment.BOTTOM,
                        scale = 1.0 * (config.spacing.globalScale / 100.0)
                    ),
                    onClick = { config::spacing.jumpToEditor() }
                ),
                blockBottomHover = false
            ),
            config.color.backgroundColor.toChromaColor(),
            padding = 10
        )

        // change global wardrobe scale if its taller or wider than the screen
        if (fullRenderable.width > gui.width || fullRenderable.height > gui.height) {
            if (config.spacing.globalScale <= 1) return
            val newScale = (config.spacing.globalScale * (0.9 / max(
                fullRenderable.width.toDouble() / gui.width,
                fullRenderable.height.toDouble() / gui.height
            ))).toInt()
            config.spacing.globalScale = newScale
            ChatUtils.clickableUserError(
                "Auto-set your Global Scale in custom wardrobe, as it was too tall/wide.",
                onClick = { config::spacing.jumpToEditor() }
            )
            update()
            return
        }

        val (width, height) = fullRenderable.width to fullRenderable.height
        val pos = Position((gui.width - width) / 2, (gui.height - height) / 2)

        pos.renderRenderable(fullRenderable, posLabel = "Wardrobe Overlay", addToGuiManager = false)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) {
                reset()
            }
        }
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        update()
    }

    @SubscribeEvent
    fun onWardrobeUpdate(event: WardrobeUpdateEvent) {
        if (!isEnabled()) return
        update()
    }


    private fun update() {
        displayRenderable = createRenderables()
        buttonsRenderable = addButtons()
    }

    private fun createRenderables(): Renderable {
        var list = WardrobeAPI.wardrobeSlots

        var wardrobeWarning = ""

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

        val totalPlayers = list.size
        val maxPlayersPerRow = config.spacing.maxPlayersPerRow
        val containerWidth = (config.spacing.slotWidth * (config.spacing.globalScale / 100.0)).toInt()
        val containerHeight = (config.spacing.slotHeight * (config.spacing.globalScale / 100.0)).toInt()
        val playerWidth = (containerWidth * config.spacing.playerScale) / 100.0
        val horizontalSpacing = (config.spacing.horizontalSpacing * (config.spacing.globalScale / 100.0)).toInt()
        val verticalSpacing = (config.spacing.verticalSpacing * (config.spacing.globalScale / 100.0)).toInt()
        val wardrobeWarningScale = 3.0 * (config.spacing.globalScale / 100.0)

        val rows = ceil(totalPlayers.toDouble() / maxPlayersPerRow).toInt()

        if (wardrobeWarning.isNotEmpty()) {
            val warningRenderable = Renderable.string(
                wardrobeWarning,
                wardrobeWarningScale,
                horizontalAlign = HorizontalAlignment.CENTER
            )
            return warningRenderable
        } else {
            val rowsRenderables = mutableListOf<Renderable>()

            for (row in 0 until rows) {
                val playersInRow =
                    if (row != rows - 1 || totalPlayers % maxPlayersPerRow == 0) maxPlayersPerRow else totalPlayers % maxPlayersPerRow

                val slotsRenderables = mutableListOf<Renderable>()

                for (playerIndex in 0 until playersInRow) {
                    var scale = playerWidth

                    val wardrobeSlot = list[maxPlayersPerRow * row + playerIndex]


                    val armorTooltipRenderable = {
                        val loreList = mutableListOf<Renderable>()
                        val height = containerHeight - 3

                        // This is needed to keep the total size of the renderable the same as the others
                        val hoverableSizes = MutableList(4) { height / 4 }
                        for (k in 0 until height % 4) hoverableSizes[k]++

                        for (armorIndex in 0 until 4) {
                            val stack = wardrobeSlot.getArmor()[armorIndex]?.copy()
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
                        topLayerRenderable = addSlotHoverableButtons(wardrobeSlot),
                        hoveredColor = getWardrobeSlotColor(wardrobeSlot),
                        borderOutlineThickness = config.spacing.outlineThickness,
                        borderOutlineBlur = config.spacing.outlineBlur,
                        onClick = {
                            clickWardrobeSlot(wardrobeSlot)
                        }
                    )

                    val fakePlayer = getFakePlayer()

                    fakePlayer.inventory.armorInventory =
                        wardrobeSlot.getArmor().map { it?.copy()?.removeEnchants() }.reversed().toTypedArray()

                    val playerColor = if (!wardrobeSlot.isInCurrentPage()) {
                        scale *= 0.9
                        Color.GRAY.withAlpha(100)
                    } else null

                    val playerRenderable = Renderable.player(
                        fakePlayer,
                        config.eyesFollowMouse,
                        width = containerWidth,
                        height = containerHeight,
                        entityScale = scale.toInt(),
                        padding = 0,
                        color = playerColor,
                    )

                    val slotRenderable = Renderable.doubleLayered(playerBackground, playerRenderable, false)

                    slotsRenderables.add(slotRenderable)
                }

                val rowRenderable = Renderable.horizontalContainer(slotsRenderables, horizontalSpacing)

                rowsRenderables.add(rowRenderable)
            }

            val allSlotsRenderable = Renderable.verticalContainer(
                rowsRenderables,
                verticalSpacing,
                horizontalAlign = HorizontalAlignment.CENTER
            )

            return allSlotsRenderable
        }
    }

    private fun reset() {
        inCustomWardrobe = false
        editMode = false
        displayRenderable = null
        buttonsRenderable = null
    }

    private fun addButtons(): Renderable {
        val horizontalSpacing = (config.spacing.buttonHorizontalSpacing * (config.spacing.globalScale / 100.0)).toInt()
        val verticalSpacing = (config.spacing.buttonVerticalSpacing * (config.spacing.globalScale / 100.0)).toInt()

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
                editMode = true
            }
        )

        val row = Renderable.horizontalContainer(
            listOf(backButton, exitButton, onlyFavoriteButton),
            horizontalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        val total = Renderable.verticalContainer(
            listOf(row, editButton),
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER
        )

        return total
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

    private fun clickWardrobeSlot(wardrobeSlot: WardrobeAPI.WardrobeSlot) {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = currentPage ?: return
        val windowId = getWindowId() ?: -1
        if (wardrobeSlot.isInCurrentPage()) {
            currentWardrobeSlot = if (wardrobeSlot.isCurrentSlot()) null
            else wardrobeSlot.id
            clickSlot(wardrobeSlot.inventorySlot, windowId)
        } else {
            if (wardrobeSlot.page < wardrobePage) {
                currentPage = wardrobePage - 1
                clickSlot(previousPageSlot, windowId)
            } else if (wardrobeSlot.page > wardrobePage) {
                currentPage = wardrobePage + 1
                clickSlot(nextPageSlot, windowId)
            }
        }
        update()
    }

    private fun getFakePlayer(): EntityOtherPlayerMP {
        val mc = Minecraft.getMinecraft()
        return object : EntityOtherPlayerMP(
            mc.theWorld,
            mc.thePlayer.gameProfile
        ) {
            override fun getLocationSkin() =
                mc.thePlayer.locationSkin ?: DefaultPlayerSkin.getDefaultSkin(mc.thePlayer.uniqueID)

            override fun getTeam() = object : ScorePlayerTeam(null, null) {
                override fun getNameTagVisibility() = EnumVisible.NEVER
            }

            override fun isWearing(part: EnumPlayerModelParts?) =
                mc.thePlayer.isWearing(part) && part != EnumPlayerModelParts.CAPE
        }
    }

    private fun getWardrobeSlotColor(wardrobeSlot: WardrobeAPI.WardrobeSlot): Color {
        val color = if (wardrobeSlot.isInCurrentPage()) {
            if (wardrobeSlot.isCurrentSlot()) config.color.equippedColor.toChromaColor()
            else if (wardrobeSlot.favorite && !config.onlyFavorites) config.color.favoriteColor.toChromaColor()
            else config.color.samePageColor.toChromaColor()
        } else {
            if (wardrobeSlot.isCurrentSlot()) config.color.equippedColor.toChromaColor().darker()
            else if (wardrobeSlot.favorite && !config.onlyFavorites) config.color.favoriteColor.toChromaColor().darker()
            else config.color.otherPageColor.toChromaColor()
        }
        return Color(color.withAlpha(170), true)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe() && !editMode
}
