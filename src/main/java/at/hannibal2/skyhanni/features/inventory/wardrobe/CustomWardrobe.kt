package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
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
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

class CustomWardrobe {

    private val config get() = SkyHanniMod.feature.inventory.customWardrobe
    private var display = emptyList<Triple<Position, Renderable, Int?>>()
    private var renderablesCache = emptyList<Triple<Position, Renderable, Int?>>()
    private var tempToggleShowOverlay = true
    private var favoriteToggle = false
    private var onlyFavoriteToggle = false

    private var itemPriceCache = mutableMapOf<String?, Double>()
    private var fakePlayerCache = mutableMapOf<Int, AbstractClientPlayer>()

    private var hoveredSlot: Int? = null

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        var list = WardrobeAPI.wardrobeSlots.filter { !it.locked }

        if (!tempToggleShowOverlay) return
        inCustomWardrobe = true

        var wardrobeWarning = false
        var wardrobeWarningText = ""

        if (list.isEmpty()) {
            wardrobeWarning = true
            wardrobeWarningText = "§cYour wardrobe is empty :("
        }

        if (config.hideEmptySlots) {
            list = list.filter { !it.isEmpty() }
            if (list.isEmpty()) {
                wardrobeWarning = true
                wardrobeWarningText = "§cAll slots are empty :("
            }
        }
        if (onlyFavoriteToggle) {
            list = list.filter { it.favorite || it.isCurrentSlot() }
            if (list.isEmpty()) {
                wardrobeWarning = true
                wardrobeWarningText = "§cDidnt set any favorites"
            }
        }

        val gui = event.gui
        val centerX = gui.width / 2
        val centerY = gui.height / 2
        val totalPlayers = list.size
        val maxPlayersPerRow = 9
        val playerWidth = 50
        val playerHeight = 2 * playerWidth
        val horizontalSpacing = 20
        val verticalSpacing = 20

        val rows = ceil(totalPlayers.toDouble() / maxPlayersPerRow).toInt()
        val totalHeight = rows * playerHeight + (rows - 1) * (verticalSpacing + 1)

        val startY = centerY + playerHeight - totalHeight / 2

        if (wardrobeWarning) {
            val warningRenderable = Renderable.string(wardrobeWarningText)
            val warningPos = Position(centerX - (warningRenderable.width * 3) / 2, centerY - 70, 3f, true);
            renderablesCache += Triple(warningPos, warningRenderable, 0)
            renderablesCache += addButtons(gui.width, gui.height, totalHeight)
            event.cancel()
            reset()
            return
        }

        GlStateManager.pushMatrix()
        GlStateManager.color(1f, 1f, 1f, 1f)

        if (renderablesCache.isEmpty()) {
            renderablesCache += addButtons(gui.width, gui.height, totalHeight)

            var slot = 0
            for (row in 0 until rows) {
                val playersInRow =
                    if (row != rows - 1 || totalPlayers % maxPlayersPerRow == 0) maxPlayersPerRow else totalPlayers % maxPlayersPerRow
                val totalWidth = playersInRow * playerWidth + (playersInRow - 1) * (horizontalSpacing + 1)

                val startX = centerX - (totalWidth - playerWidth) / 2
                val playerY = startY + row * ((playerHeight + verticalSpacing) + 1)

                for (i in 0 until playersInRow) {
                    val playerX = startX + i * ((playerWidth + horizontalSpacing) + 1)
                    var scale = playerWidth.toDouble()

                    val wardrobeSlot = list[slot]

                    val fakePlayer = wardrobeSlot.getFakePlayer()

                    fakePlayer.inventory.armorInventory =
                        wardrobeSlot.getArmor().map { it?.copy()?.removeEnchants() }.reversed().toTypedArray()

                    val padding = 10
                    val pos = Position(playerX - padding - playerWidth / 2, playerY - playerHeight - padding)
                    val containerWidth = playerWidth + 2 * padding
                    val containerHeight = playerHeight + 2 * padding

                    /*
                    estimated wardrobe price hover text
                    val estimatedWardrobePriceRenderable = {
                        val placeHolder = Renderable.placeholder(containerWidth, containerHeight)
                        if (wardrobeSlot.getArmor().any { it != null }) {
                            val lore = createWardrobePriceLore(wardrobeSlot)
                            Renderable.hoverTips(placeHolder, lore)
                        } else {
                            placeHolder
                        }
                    }
                    */

                    val armorTooltipRenderable = {
                        val loreList = mutableListOf<List<Renderable>>()
                        val height = containerHeight - 3

                        // this is needed to keep the total size of the renderable the same as the others
                        val hoverableSizes = MutableList(4) { height / 4 }
                        for (k in 0 until height % 4) hoverableSizes[k]++

                        for (j in 0 until 4) {
                            val stack = wardrobeSlot.getArmor()[j]?.copy()
                            if (stack == null) {
                                loreList.add(listOf(Renderable.placeholder(containerWidth, hoverableSizes[j])))
                            } else {
                                loreList.add(
                                    listOf(
                                        Renderable.hoverable(
                                            Renderable.hoverTips(
                                                Renderable.placeholder(containerWidth, hoverableSizes[j]),
                                                stack.getTooltip(Minecraft.getMinecraft().thePlayer, false)
                                            ),
                                            Renderable.placeholder(containerWidth, hoverableSizes[j]),
                                            bypassChecks = true
                                        )
                                    )
                                )
                            }
                        }
                        Renderable.table(loreList, yPadding = 1)
                    }

                    val renderable = createHoverableRenderable(
                        armorTooltipRenderable.invoke(),
                        hoveredColor = getWardrobeSlotColor(wardrobeSlot),
                        borderOutlineThickness = config.color.outlineThickness,
                        borderOutlineBlur = config.color.outlineBlur,
                        onClick = {
                            clickWardrobeSlot(wardrobeSlot)
                        },
                        onHover = {
                            hoveredSlot = wardrobeSlot.id
                        }
                    )
                    renderablesCache += Triple(pos, renderable, wardrobeSlot.id)

                    val color = if (!wardrobeSlot.isInCurrentPage()) {
                        scale *= 0.9
                        Color.GRAY.withAlpha(100)
                    } else null

                    renderablesCache += Triple(
                        Position(playerX, playerY), Renderable.entity(
                            fakePlayer,
                            config.eyesFollowMouse,
                            scale = scale.toInt(),
                            color = color
                        ), wardrobeSlot.id
                    )
                    slot++
                }
            }
        }
        display = renderablesCache
        GlStateManager.popMatrix()

        event.cancel()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) {
                inCustomWardrobe = false
                tempToggleShowOverlay = true
                favoriteToggle = false
                itemPriceCache = mutableMapOf()
                display = mutableListOf()
                reset()
            }
        }
    }

    private fun reset() {
        renderablesCache = mutableListOf()
        fakePlayerCache = mutableMapOf()
    }

    private fun addButtons(screenWidth: Int, screenHeight: Int, playerHeight: Int) = buildList {
        val buttonWidth = 25
        val centerX = screenWidth / 2
        val buttonY = screenHeight / 2 + playerHeight / 2 + 30
        val padding = 10

        val renderables = listOf(
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    listOf(
                        "§aToggle Wardrobe Overlay for one time",
                        " §7This will hide the overlay",
                        " §7until you open the wardrobe again",
                    )
                ),
                hoveredColor = Color.BLACK,
                borderOutlineThickness = 2,
                onClick = {
                    tempToggleShowOverlay = false
                    inCustomWardrobe = false
                    reset()
                    display = mutableListOf()
                }
            ),
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    listOf(
                        "§aToggle Favorite Selector",
                        " §7This will allow you to toggle",
                        " §7the favorite status of the armor pieces",
                    )
                ),
                hoveredColor = if (favoriteToggle) Color.GREEN else Color.RED,
                borderOutlineThickness = 2,
                onClick = { favoriteToggle = !favoriteToggle }
            ),
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    listOf(
                        "§aToggle Only Favorite",
                        " §7This will allow you to toggle",
                        " §7only showing favorite armors",
                    )
                ),
                hoveredColor = if (onlyFavoriteToggle) Color.GREEN else Color.RED,
                borderOutlineThickness = 2,
                onClick = {
                    onlyFavoriteToggle = !onlyFavoriteToggle
                    reset()
                }
            ),
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    listOf("§aGo Back", " §7To SkyBlock Menu")
                ),
                hoveredColor = Color.BLACK,
                borderOutlineThickness = 2,
                onClick = {
                    clickSlot(48, getWindowId() ?: -1)
                }
            ),
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    listOf("§cClose")
                ),
                hoveredColor = Color.BLACK,
                borderOutlineThickness = 2,
                onClick = {
                    clickSlot(49, getWindowId() ?: -1)
                }
            ),
        )

        val totalWidth = renderables.sumOf { it.width } + (renderables.size - 1) * padding
        val startX = centerX - totalWidth / 2

        for ((index, renderable) in renderables.withIndex()) {
            add(Triple(Position(startX + index * (renderable.width + padding), buttonY), renderable, 0))
        }
    }

    private fun WardrobeAPI.WardrobeSlot.getFakePlayer(): AbstractClientPlayer =
        fakePlayerCache.getOrPut(this.id) {
            val mc = Minecraft.getMinecraft()
            object : AbstractClientPlayer(
                mc.theWorld,
                mc.thePlayer.gameProfile
            ) {
                override fun getLocationSkin(): ResourceLocation {
                    // TODO: where second layer
                    // please someone fixed this ive been trying for 5h
                    return mc.thePlayer.locationSkin
                        ?: DefaultPlayerSkin.getDefaultSkin(mc.thePlayer.uniqueID)
                }

                override fun getName(): String {
                    // Future TODO: Empa wants to add per slot names
                    return ""
                }
            }
        }

    private fun createHoverableRenderable(
        hoveredRenderable: Renderable,
        topLayerRenderable: Renderable = Renderable.placeholder(0, 0),
        hoveredColor: Color,
        unHoveredColor: Color = hoveredColor,
        borderOutlineThickness: Int,
        borderOutlineBlur: Float = 0f,
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
                padding = 0,
                topOutlineColor = config.color.topBorderColor.toChromaColorInt(),
                bottomOutlineColor = config.color.bottomBorderColor.toChromaColorInt(),
                borderOutlineThickness = borderOutlineThickness,
                blur = borderOutlineBlur
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.placeholder(hoveredRenderable.width, hoveredRenderable.height),
                unHoveredColor,
                padding = 0
            ),
            onHover = { onHover() }
        )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        GlStateManager.pushMatrix()
        for ((pos, renderable, _) in display.sortedBy { if (it.third == hoveredSlot) 1 else 0 }) {
            GlStateManager.color(1f, 1f, 1f, 1f)
            pos.renderRenderables(listOf(renderable), posLabel = "Wardrobe Overlay")
        }
        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!inWardrobe()) return
        reset()
    }

    private fun clickWardrobeSlot(wardrobeSlot: WardrobeAPI.WardrobeSlot) {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = currentPage ?: return
        val windowId = getWindowId() ?: -1
        if (favoriteToggle) {
            wardrobeSlot.favorite = !wardrobeSlot.favorite
            reset()
            return
        }
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
        reset()
    }

    private fun getWardrobeSlotColor(wardrobeSlot: WardrobeAPI.WardrobeSlot): Color {
        val color = if (wardrobeSlot.isInCurrentPage()) {
            if (wardrobeSlot.isCurrentSlot()) config.color.equippedColor.toChromaColor()
            else if (wardrobeSlot.favorite && !onlyFavoriteToggle) config.color.favoriteColor.toChromaColor()
            else config.color.samePageColor.toChromaColor()
        } else {
            if (wardrobeSlot.isCurrentSlot()) config.color.equippedColor.toChromaColor().darker()
            else if (wardrobeSlot.favorite && !onlyFavoriteToggle) config.color.favoriteColor.toChromaColor().darker()
            else config.color.otherPageColor.toChromaColor()
        }
        return Color(color.withAlpha(170), true)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe()
}
