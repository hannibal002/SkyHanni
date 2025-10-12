package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
//#if MC < 1.21
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
//#else
//$$ import net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity
//#endif

fun Renderable.Companion.fakePlayer(
    player: EntityPlayer,
    followMouse: Boolean = false,
    eyesX: Float = 0f,
    eyesY: Float = 0f,
    width: Int = 50,
    height: Int = 100,
    entityScale: Int = 30,
    padding: Int = 5,
    color: Color? = null,
    colorCondition: () -> Boolean = { true },
) = object : Renderable {
    override val width = width + 2 * padding
    override val height = height + 2 * padding
    override val horizontalAlign = HorizontalAlignment.LEFT
    override val verticalAlign = VerticalAlignment.TOP
    val playerHeight = entityScale * 2
    val playerX = width / 2 + padding
    val playerY = height / 2 + playerHeight / 2 + padding

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        GlStateManager.color(1f, 1f, 1f, 1f)
        if (color != null) RenderLivingEntityHelper.setEntityColor(player, color, colorCondition)
        val mouse = currentRenderPassMousePosition ?: return
        val (mouseXRelativeToPlayer, mouseYRelativeToPlayer) = if (followMouse) {
            val newOffsetX = (mouseOffsetX + playerX - mouse.first).toFloat()
            val newOffsetY = (mouseOffsetY + playerY - mouse.second - 1.62 * entityScale).toFloat()
            newOffsetX to newOffsetY
        } else eyesX to eyesY
        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(0f, 0f, 100f)
        //#if MC < 1.21
        drawEntityOnScreen(
            playerX,
            playerY,
            entityScale,
            mouseXRelativeToPlayer,
            mouseYRelativeToPlayer,
            player,
        )
        //#else
        //$$ drawEntity(
        //$$     DrawContextUtils.drawContext,
        //$$     padding,
        //$$     padding,
        //$$     padding + width,
        //$$     padding + height,
        //$$     entityScale,
        //$$     0.0625f,
        //$$     if (followMouse) mouse.first - mouseOffsetX.toFloat() else eyesX,
        //$$     if (followMouse) mouse.second - mouseOffsetY.toFloat() else eyesY,
        //$$     player
        //$$ )
        //#endif
        DrawContextUtils.popMatrix()
    }
}


