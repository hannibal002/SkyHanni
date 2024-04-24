package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.currentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.favorite
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.getArmor
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isCurrentSlot
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isInCurrentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.locked
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

class WardrobeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.wardrobeOverlay
    private var display = emptyList<Pair<Position, Renderable>>()
    private var renderablesCache = emptyList<Pair<Position, Renderable>>()
    private var tempToggleShowOverlay = true
    private var favoriteToggle = false

    private var itemPriceCache = mutableMapOf<String?, Double>()
    private var fakePlayerCache = mutableMapOf<Int, AbstractClientPlayer>()

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        val list = WardrobeAPI.wardrobeSlots.filter { !it.locked }
        if (list.isEmpty()) return

        display = emptyList()

        if (!tempToggleShowOverlay) return

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

        display += addButtons(gui.width, gui.height, totalHeight)

        val isRenderableCacheEmpty = renderablesCache.isEmpty()

        GlStateManager.pushMatrix()
        GlStateManager.color(1f, 1f, 1f, 1f)

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

                if (isRenderableCacheEmpty) {
                    val padding = 10
                    val pos = Position(playerX - padding - playerWidth / 2, playerY - playerHeight - padding)
                    val containerWidth = playerWidth + 2 * padding
                    val containerHeight = playerHeight + 2 * padding

                    val hoverRenderable = {
                        if (wardrobeSlot.getArmor().any { it != null }) {
                            val lore = mutableListOf<String>()
                            lore.add("§aEstimated Armor Value:")

                            var totalPrice = 0.0
                            for (item in wardrobeSlot.getArmor().filterNotNull()) {
                                val price = item.getPrice()
                                totalPrice += price
                                lore.add("  §7- ${item.name}: §6${NumberUtil.format(price)}")
                            }

                            lore.add(" §aTotal Value: §6§l${NumberUtil.format(totalPrice)} coins")

                            Renderable.toolTipContainer(lore, containerWidth, containerHeight)
                        } else {
                            Renderable.placeholder(containerWidth, containerHeight)
                        }
                    }

                    val renderable = Renderable.clickAndHoverable(
                        Renderable.drawInsideRoundedRect(
                            hoverRenderable.invoke(),
                            getWardrobeSlotColor(wardrobeSlot, true),
                            padding = 0
                        ),
                        Renderable.drawInsideRoundedRect(
                            Renderable.placeholder(containerWidth, containerHeight),
                            getWardrobeSlotColor(wardrobeSlot, false),
                            padding = 0
                        ),
                        onClick = {
                            clickWardrobeSlot(wardrobeSlot)
                        },
                    )
                    renderablesCache += pos to renderable
                }

                // Calculate the new mouse position relative to the player
                val mouseXRelativeToPlayer = (playerX - event.mouseX).toFloat()
                val mouseYRelativeToPlayer = (playerY - event.mouseY - 1.62 * scale).toFloat()

                val eyesX = if (config.eyesFollowMouse) mouseXRelativeToPlayer else 0f
                val eyesY = if (config.eyesFollowMouse) mouseYRelativeToPlayer else 0f

                val color = if (!wardrobeSlot.isInCurrentPage()) {
                    scale *= 0.9
                    Color.GRAY.withAlpha(100)
                } else null

                display += Position(playerX, playerY) to Renderable.entity(
                    fakePlayer,
                    eyesX,
                    eyesY,
                    scale.toInt(),
                    color
                )
                slot++
            }
        }
        renderablesCache.forEach { display += it.first to it.second }

        GlStateManager.popMatrix()

        event.cancel()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) {
                tempToggleShowOverlay = true
                favoriteToggle = false
                itemPriceCache = mutableMapOf()
                fakePlayerCache = mutableMapOf()
                renderablesCache = mutableListOf()
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
        val buttonY = screenHeight / 2 + playerHeight / 2 + 15
        val padding = 10

        val renderables = listOf(
            Renderable.drawInsideRoundedRect(
                Renderable.clickAndHoverable(
                    Renderable.toolTipContainer(
                        listOf(
                            "§aToggle Wardrobe Overlay for one time",
                            " §7This will hide the overlay",
                            " §7until you open the wardrobe again",
                        ),
                        buttonWidth,
                        buttonWidth,
                    ),
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    onClick = { tempToggleShowOverlay = false },
                ),
                Color.BLACK,
                padding = 0,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.clickAndHoverable(
                    Renderable.toolTipContainer(
                        listOf(
                            "§aToggle Favorite Selector",
                            " §7This will allow you to toggle",
                            " §7the favorite status of the armor pieces",
                        ),
                        buttonWidth,
                        buttonWidth,
                    ),
                    Renderable.placeholder(buttonWidth, buttonWidth),
                    onClick = { favoriteToggle = !favoriteToggle },
                ),
                if (favoriteToggle) Color.GREEN else Color.RED,
                padding = 0,
            ),
        )

        val totalWidth = renderables.sumOf { it.width } + (renderables.size - 1) * padding
        val startX = centerX - totalWidth / 2

        for ((index, renderable) in renderables.withIndex()) {
            add(Position(startX + index * (renderable.width + padding), buttonY) to renderable)
        }
    }

    private fun ItemStack.getPrice(): Double =
        itemPriceCache.getOrPut(this.getItemUuid()) { EstimatedItemValueCalculator.calculate(this).first }

    private fun WardrobeAPI.WardrobeSlot.getFakePlayer(): AbstractClientPlayer =
        fakePlayerCache.getOrPut(this.id) {
            val playerTexture = Minecraft.getMinecraft().thePlayer.locationSkin
            object : AbstractClientPlayer(
                Minecraft.getMinecraft().theWorld,
                Minecraft.getMinecraft().thePlayer.gameProfile
            ) {
                override fun getLocationSkin(): ResourceLocation {
                    return playerTexture
                        ?: DefaultPlayerSkin.getDefaultSkin(Minecraft.getMinecraft().thePlayer.uniqueID)
                }

                override fun getName(): String {
                    // Future TODO: Empa wants to add per slot names
                    return ""
                }
            }
        }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        GlStateManager.pushMatrix()
        for ((pos, renderable) in display) {
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
        if (favoriteToggle) {
            wardrobeSlot.favorite = !wardrobeSlot.favorite
            reset()
            return
        }
        if (wardrobeSlot.isInCurrentPage()) {
            clickSlot(wardrobeSlot.inventorySlot, getWindowId() ?: -1)
        } else {
            if (wardrobeSlot.page < wardrobePage) clickSlot(previousPageSlot, getWindowId() ?: -1)
            else if (wardrobeSlot.page > wardrobePage) clickSlot(nextPageSlot, getWindowId() ?: -1)
        }
    }

    private fun getWardrobeSlotColor(wardrobeSlot: WardrobeAPI.WardrobeSlot, isHovered: Boolean): Color {
        val color = if (wardrobeSlot.isInCurrentPage()) {
            if (isHovered) LorenzColor.GOLD.toColor()
            else if (wardrobeSlot.isCurrentSlot()) LorenzColor.GREEN.toColor()
            else if (wardrobeSlot.favorite) LorenzColor.RED.toColor()
            else LorenzColor.BLUE.toColor()
        } else {
            if (isHovered) LorenzColor.GOLD.toColor().darker()
            else if (wardrobeSlot.isCurrentSlot()) LorenzColor.GREEN.toColor().darker()
            else if (wardrobeSlot.favorite) LorenzColor.RED.toColor().darker()
            else LorenzColor.BLACK.toColor()
        }
        return Color(color.withAlpha(170), true)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe()
}
