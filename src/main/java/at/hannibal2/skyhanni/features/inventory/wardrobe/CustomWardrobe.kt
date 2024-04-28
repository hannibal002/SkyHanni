package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
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
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

class CustomWardrobe {

    private val config get() = SkyHanniMod.feature.inventory.customWardrobe

    private var display = emptyList<Triple<Position, Renderable, Int>>()
    private var tempToggleShowOverlay = true

    private var hoveredSlot: Int? = null

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        if (tempToggleShowOverlay) event.cancel()
        update()
    }

    private fun update() {
        display = createRenderables()
    }

    private fun createRenderables() = buildList {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return@buildList

        var list = WardrobeAPI.wardrobeSlots.filter { !it.locked }

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
        if (config.onlyFavorites) {
            list = list.filter { it.favorite || it.isCurrentSlot() }
            if (list.isEmpty()) {
                wardrobeWarning = true
                wardrobeWarningText = "§cDidn't set any favorites"
            }
        }

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

        addButtons(gui.width, gui.height, totalHeight).forEach { add(it) }

        if (wardrobeWarning) {
            val warningRenderable = Renderable.string(wardrobeWarningText)
            val warningPos = Position(centerX - (warningRenderable.width * 3) / 2, centerY - 70, 3f, true)
            add(Triple(warningPos, warningRenderable, 0))
            return@buildList
        }

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

                val armorTooltipRenderable = {
                    val loreList = mutableListOf<Renderable>()
                    val height = containerHeight - 3

                    // this is needed to keep the total size of the renderable the same as the others
                    val hoverableSizes = MutableList(4) { height / 4 }
                    for (k in 0 until height % 4) hoverableSizes[k]++

                    for (j in 0 until 4) {
                        val stack = wardrobeSlot.getArmor()[j]?.copy()
                        if (stack == null) {
                            loreList.add(Renderable.placeholder(containerWidth, hoverableSizes[j]))
                        } else {
                            loreList.add(
                                Renderable.hoverable(
                                    Renderable.hoverTips(
                                        Renderable.placeholder(containerWidth, hoverableSizes[j]),
                                        stack.getTooltip(Minecraft.getMinecraft().thePlayer, false)
                                    ),
                                    Renderable.placeholder(containerWidth, hoverableSizes[j]),
                                    bypassChecks = true
                                )
                            )
                        }
                    }
                    Renderable.verticalContainer(loreList, spacing = 1)
                }

                val renderable = createHoverableRenderable(
                    armorTooltipRenderable.invoke(),
                    topLayerRenderable = addSlotHoverableButtons(wardrobeSlot),
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
                add(Triple(pos, renderable, wardrobeSlot.id))

                val color = if (!wardrobeSlot.isInCurrentPage()) {
                    scale *= 0.9
                    Color.GRAY.withAlpha(100)
                } else null

                add(
                    Triple(
                        Position(playerX, playerY), Renderable.entity(
                            fakePlayer,
                            config.eyesFollowMouse,
                            scale = scale.toInt(),
                            color = color
                        ), wardrobeSlot.id
                    )
                )
                slot++
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) {
                reset()
            }
        }
    }

    private fun reset() {
        inCustomWardrobe = false
        tempToggleShowOverlay = true
        display = mutableListOf()
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
                    reset()
                    tempToggleShowOverlay = false
                }
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
                hoveredColor = if (config.onlyFavorites) Color.GREEN else Color.RED,
                borderOutlineThickness = 2,
                onClick = {
                    config.onlyFavorites = !config.onlyFavorites
                    update()
                }
            ),
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.itemStack(
                        ItemStack(Items.arrow),
                        (buttonWidth - 1.5) / 15.5,
                        0
                    ),
                    listOf("§aGo Back", " §7To SkyBlock Menu")
                ),
                Renderable.itemStack(
                    ItemStack(Items.arrow),
                    (buttonWidth - 1.5) / 15.5,
                    0
                ),
                hoveredColor = Color.BLACK,
                borderOutlineThickness = 2,
                onClick = {
                    clickSlot(48, getWindowId() ?: -1)
                    reset()
                    currentPage = null
                }
            ),
            createHoverableRenderable(
                Renderable.hoverTips(
                    Renderable.itemStack(
                        ItemStack(Blocks.barrier),
                        (buttonWidth - 1.5) / 15.5,
                        0
                    ),
                    listOf("§cClose")
                ),
                Renderable.itemStack(
                    ItemStack(Blocks.barrier),
                    (buttonWidth - 1.5) / 15.5,
                    0
                ),
                hoveredColor = Color.BLACK,
                borderOutlineThickness = 2,
                onClick = {
                    clickSlot(49, getWindowId() ?: -1)
                    reset()
                }
            ),
        )

        val totalWidth = renderables.sumOf { it.width } + (renderables.size - 1) * padding
        val startX = centerX - totalWidth / 2

        for ((index, renderable) in renderables.withIndex()) {
            add(Triple(Position(startX + index * (renderable.width + padding), buttonY), renderable, 0))
        }
    }

    private fun WardrobeAPI.WardrobeSlot.getFakePlayer(): EntityOtherPlayerMP {
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

    private fun addSlotHoverableButtons(wardrobeSlot: WardrobeAPI.WardrobeSlot): Renderable {
        val list = mutableListOf<Renderable>()
        list.add(
            Renderable.clickable(
                Renderable.hoverable(
                    Renderable.string(
                        (if (wardrobeSlot.favorite) "§c" else "§7") + "❤",
                        1.5,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        verticalAlign = RenderUtils.VerticalAlignment.CENTER
                    ),
                    Renderable.string(
                        (if (wardrobeSlot.favorite) "§4" else "§8") + "❤",
                        1.5,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        verticalAlign = RenderUtils.VerticalAlignment.CENTER
                    )
                ),
                onClick = {
                    wardrobeSlot.favorite = !wardrobeSlot.favorite
                    update()
                }
            )
        )

        if (config.estimatedValue) {
            list.add(
                if (wardrobeSlot.getArmor().any { it != null }) {
                    val lore = createWardrobePriceLore(wardrobeSlot)
                    Renderable.hoverTips(
                        Renderable.string(
                            "§2$",
                            1.5,
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                            verticalAlign = RenderUtils.VerticalAlignment.CENTER
                        ), lore
                    )
                } else {
                    Renderable.placeholder(0, 0)
                }
            )
        }

        return Renderable.verticalContainer(list, 1, RenderUtils.HorizontalAlignment.CENTER)
    }

    private fun createHoverableRenderable(
        hoveredRenderable: Renderable,
        unhoveredRenderable: Renderable = Renderable.placeholder(hoveredRenderable.width, hoveredRenderable.height),
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
                unhoveredRenderable,
                unHoveredColor,
                padding = 0
            ),
            onHover = { onHover() }
        )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!tempToggleShowOverlay) return

        display.ifEmpty { update() }
        if (display.isEmpty()) return

        for ((pos, renderable, _) in display.sortedBy { if (it.third == hoveredSlot) 1 else 0 }) {
            GlStateManager.color(1f, 1f, 1f, 1f)
            pos.renderRenderables(listOf(renderable), posLabel = "Wardrobe Overlay")
        }
    }

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

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe()
}
