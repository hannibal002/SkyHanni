package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.armor
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.currentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.favorite
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isCurrentSlot
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isInCurrentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.locked
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
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
    private var tempToggleShowOverlay = true
    private var favoriteToggle = false

    private var itemPriceCache = mutableMapOf<ItemStack, Double>()

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        val list = WardrobeAPI.wardrobeSlots.filter { !it.locked }
        if (list.isEmpty()) return

        display = emptyList()

        if (!tempToggleShowOverlay) return

        val gui = event.gui
        val player = Minecraft.getMinecraft().thePlayer
        val centerX = gui.width / 2
        val centerY = gui.height / 2
        //val totalPlayers = 18
        val totalPlayers = list.size
        val maxPlayersPerRow = 9
        val playerWidth = 50
        val playerHeight = 2 * playerWidth
        val horizontalSpacing = 20
        val verticalSpacing = 20

        val rows = ceil(totalPlayers.toDouble() / maxPlayersPerRow).toInt()
        val totalHeight = rows * playerHeight + (rows - 1) * verticalSpacing

        val startY = centerY + playerHeight - totalHeight / 2


        val tempTogglePos = Position((gui.width * 0.85).toInt(), (gui.height * 0.9).toInt())
        val tempToggleRenderable = Renderable.horizontalContainer(
            listOf(
                Renderable.drawInsideRoundedRect(
                    Renderable.clickable(
                        Renderable.emptyContainer(30, 30),
                        bypassChecks = true,
                        onClick = {
                            ChatUtils.chat("Clicked on wardrobe toggle")
                            tempToggleShowOverlay = false
                        },
                    ),
                    Color.BLACK,
                ), Renderable.string("Temp toggle")
            ), spacing = 10, verticalAlign = RenderUtils.VerticalAlignment.CENTER
        )
        display += tempTogglePos to tempToggleRenderable

        val favoriteTogglePos = Position(tempTogglePos.rawX, tempTogglePos.rawY - 50)
        val favoriteToggleRenderable = Renderable.horizontalContainer(
            listOf(
                Renderable.drawInsideRoundedRect(
                    Renderable.clickable(
                        Renderable.emptyContainer(30, 30),
                        bypassChecks = true,
                        onClick = {
                            ChatUtils.chat("Clicked on favorite toggle")
                            favoriteToggle = !favoriteToggle
                        },
                    ),
                    if (favoriteToggle) Color.GREEN else Color.RED
                ), Renderable.string("Favorite toggle")
            ), spacing = 10, verticalAlign = RenderUtils.VerticalAlignment.CENTER
        )
        display += favoriteTogglePos to favoriteToggleRenderable


        GlStateManager.pushMatrix()
        GlStateManager.color(1f, 1f, 1f, 1f)

        var slot = 0
        for (row in 0 until rows) {
            val playersInRow =
                if (row != rows - 1 || totalPlayers % maxPlayersPerRow == 0) maxPlayersPerRow else totalPlayers % maxPlayersPerRow
            val totalWidth = playersInRow * playerWidth + (playersInRow - 1) * horizontalSpacing

            val startX = centerX - (totalWidth - playerWidth) / 2
            val playerY = startY + row * (playerHeight + verticalSpacing)

            for (i in 0 until playersInRow) {
                val playerX = startX + i * (playerWidth + horizontalSpacing)
                var scale = playerWidth.toDouble()

                val wardrobeSlot = list[slot]

                val padding = 10
                val pos = Position(playerX - padding - playerWidth / 2, playerY - playerHeight - padding)
                val containerWidth = playerWidth + 2 * padding
                val containerHeight = playerHeight + 2 * padding


                val playerTexture = player.locationSkin
                val fakePlayer = object : AbstractClientPlayer(gui.mc.theWorld, player.gameProfile) {
                    override fun getLocationSkin(): ResourceLocation {
                        return playerTexture ?: DefaultPlayerSkin.getDefaultSkin(player.uniqueID)
                    }

                    override fun getName(): String {
                        return ""
                    }
                }

                fakePlayer.inventory.armorInventory =
                    wardrobeSlot.armor.map { it?.copy()?.removeEnchants() }.reversed().toTypedArray()

                val isInPage = wardrobeSlot.isInCurrentPage
                RenderLivingEntityHelper.removeEntityColor(fakePlayer)
                if (!isInPage) {
                    scale *= 0.9
                    RenderLivingEntityHelper.setEntityColor(
                        fakePlayer,
                        Color.GRAY.withAlpha(100),
                        ::isEnabled
                    )
                }

                val isHovered = GuiRenderUtils.isPointInRect(
                    event.mouseX,
                    event.mouseY,
                    pos.rawX,
                    pos.rawY,
                    containerWidth,
                    containerHeight
                )

                val hoverRenderable = {
                    val lore = mutableListOf<String>()
                    lore.add("§aEstimated Armor Value:")

                    var totalPrice = 0.0
                    for (item in wardrobeSlot.armor.filterNotNull()) {
                        val price = item.getPrice()
                        totalPrice += price
                        lore.add("  §7- ${item.name}: §6${NumberUtil.format(price)}")
                    }

                    if (wardrobeSlot.armor.any { it != null }) {
                        lore.add(" §aTotal Value: §6§l${NumberUtil.format(totalPrice)} coins")

                        Renderable.toolTipContainer(lore, containerWidth, containerHeight)
                    } else {
                        Renderable.emptyContainer(containerWidth, containerHeight)
                    }
                }

                val renderable = Renderable.drawInsideRoundedRect(
                    Renderable.clickAndHoverable(
                        hoverRenderable.invoke(),
                        Renderable.emptyContainer(containerWidth, containerHeight),
                        onClick = {
                            clickWardrobeSlot(wardrobeSlot)
                        }
                    ),
                    getWardrobeSlotColor(wardrobeSlot, isHovered),
                    0,
                )

                // Calculate the new mouse position relative to the player
                val mouseXRelativeToPlayer = (playerX - event.mouseX).toFloat()
                val mouseYRelativeToPlayer = (playerY - event.mouseY - 1.62 * scale).toFloat()

                val eyesX = if (config.eyesFollowMouse) mouseXRelativeToPlayer else 0f
                val eyesY = if (config.eyesFollowMouse) mouseYRelativeToPlayer else 0f

                display += Position(playerX, playerY) to Renderable.player(fakePlayer, eyesX, eyesY, scale.toInt())
                display += pos to renderable

                slot++
            }
        }

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
            }
        }
    }

    private fun ItemStack.getPrice(): Double {
        return itemPriceCache.getOrPut(this) { EstimatedItemValueCalculator.calculate(this).first }
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

    private fun clickWardrobeSlot(wardrobeSlot: WardrobeAPI.WardrobeSlot) {
        val previousPageSlot = 45
        val nextPageSlot = 53
        val wardrobePage = currentPage ?: return
        if (favoriteToggle) {
            wardrobeSlot.favorite = !wardrobeSlot.favorite
            return
        }
        if (wardrobeSlot.isInCurrentPage) {
            clickSlot(wardrobeSlot.inventorySlot, getWindowId() ?: -1)
        } else {
            if (wardrobeSlot.page < wardrobePage) clickSlot(previousPageSlot, getWindowId() ?: -1)
            else if (wardrobeSlot.page > wardrobePage) clickSlot(nextPageSlot, getWindowId() ?: -1)
        }
    }

    private fun getWardrobeSlotColor(wardrobeSlot: WardrobeAPI.WardrobeSlot, isHovered: Boolean): Color {
        val color = if (wardrobeSlot.isInCurrentPage) {
            if (isHovered) LorenzColor.GOLD.toColor()
            else if (wardrobeSlot.isCurrentSlot) LorenzColor.GREEN.toColor()
            else if (wardrobeSlot.favorite) LorenzColor.RED.toColor()
            else LorenzColor.BLUE.toColor()
        } else {
            if (isHovered) LorenzColor.GOLD.toColor().darker()
            else if (wardrobeSlot.isCurrentSlot) LorenzColor.GREEN.toColor().darker()
            else if (wardrobeSlot.favorite) LorenzColor.RED.toColor().darker()
            else LorenzColor.BLACK.toColor()
        }
        return Color(color.withAlpha(170), true)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe()

}
