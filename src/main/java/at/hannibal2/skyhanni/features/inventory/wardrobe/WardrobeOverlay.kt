package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.armor
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inWardrobe
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils.clickSlot
import at.hannibal2.skyhanni.utils.InventoryUtils.getWindowId
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.util.IChatComponent
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

class WardrobeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.wardrobeOverlay
    private var display = emptyList<Pair<Position, Renderable>>()
    private var tempToggleShowOverlay = true

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return

        display = emptyList()

        if (!tempToggleShowOverlay) return

        val gui = event.gui
        val player = Minecraft.getMinecraft().thePlayer
        val centerX = gui.width / 2
        val centerY = gui.height / 2
        val totalPlayers = 18
        val maxPlayersPerRow = 9
        val playerWidth = 50
        val playerHeight = 2 * playerWidth
        val horizontalSpacing = 20
        val verticalSpacing = 20

        val playerTexture = player.locationSkin
        val fakePlayer = object : AbstractClientPlayer(gui.mc.theWorld, player.gameProfile) {
            override fun getLocationSkin(): ResourceLocation {
                return playerTexture ?: DefaultPlayerSkin.getDefaultSkin(player.uniqueID)
            }

            override fun getName(): String {
                return ""
            }
        }

        val rows = ceil(totalPlayers.toDouble() / maxPlayersPerRow).toInt()
        val totalHeight = rows * playerHeight + (rows - 1) * verticalSpacing

        val startY = centerY + playerHeight - totalHeight / 2


        val tempTogglePos = Position((gui.width * 0.9).toInt(), (gui.height * 0.8).toInt())
        val tempToggleRenderable = Renderable.drawInsideRoundedRect(
            Renderable.clickable(
                Renderable.emptyContainer(30, 30),
                bypassChecks = true,
                onClick = {
                    ChatUtils.chat("Clicked on wardrobe toggle")
                    tempToggleShowOverlay = false
                },
            ),
            Color.BLACK,
        )

        display += tempTogglePos to tempToggleRenderable


        GlStateManager.pushMatrix()
        GlStateManager.color(1f, 1f, 1f, 1f)


        for (row in 0 until rows) {
            val playersInRow =
                if (row != rows - 1 || totalPlayers % maxPlayersPerRow == 0) maxPlayersPerRow else totalPlayers % maxPlayersPerRow
            val totalWidth = playersInRow * playerWidth + (playersInRow - 1) * horizontalSpacing

            val startX = centerX - (totalWidth - playerWidth) / 2
            val playerY = startY + row * (playerHeight + verticalSpacing)

            for (i in 0 until playersInRow) {
                val playerX = startX + i * (playerWidth + horizontalSpacing)
                val scale = playerWidth

                // Calculate the new mouse position relative to the player
                val mouseXRelativeToPlayer = (playerX - event.mouseX).toFloat()
                val mouseYRelativeToPlayer = (playerY - event.mouseY - 1.62 * scale).toFloat()

                val eyesX = if (config.eyesFollowMouse) mouseXRelativeToPlayer else 0f
                val eyesY = if (config.eyesFollowMouse) mouseYRelativeToPlayer else 0f

                fakePlayer.inventory.armorInventory =
                    WardrobeAPI.wardrobeSlots[i + row * maxPlayersPerRow].armor.toTypedArray()

                drawEntityOnScreen(
                    playerX,
                    playerY,
                    scale,
                    eyesX,
                    eyesY,
                    fakePlayer
                )

                val padding = 5
                val pos = Position(playerX - padding - playerWidth / 2, playerY - playerHeight - padding)

                val renderable = Renderable.drawInsideRoundedRect(
                    Renderable.clickAndHoverable(
                        Renderable.emptyContainer(playerWidth, playerHeight),
                        Renderable.emptyContainer(playerWidth, playerHeight),
                        onClick = {
                            ChatUtils.debug("Clicked on wardrobe player at $i")
                            clickSlot(
                                WardrobeAPI.wardrobeSlots[i].inventorySlot,
                                getWindowId() ?: -1
                            )
                        },
                        onHover = {
                            //ChatUtils.debug("Hovered over wardrobe player at $i")
                        }
                    ),
                    Color.BLACK,
                    padding,
                )

                display += pos to renderable
            }
        }

        GlStateManager.popMatrix()

        event.cancel()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (!inWardrobe()) tempToggleShowOverlay = true
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        for ((pos, renderable) in display) {
            pos.renderRenderables(listOf(renderable), posLabel = "Wardrobe Overlay")
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe()

}
