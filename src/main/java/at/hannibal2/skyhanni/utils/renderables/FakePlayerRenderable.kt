package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.entity.LivingEntity
import org.joml.Matrix3x2f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.awt.Color
import kotlin.math.atan

open class FakePlayerRenderable(
    protected val player: FakePlayer,
    private val followMouse: Boolean,
    private val eyesX: Float,
    private val eyesY: Float,
    private val entityScale: Int,
    private val padding: Int,
    private val color: Color?,
    private val colorCondition: () -> Boolean,
    rawWidth: Int,
    rawHeight: Int,
) : Renderable {

    override val width = rawWidth + 2 * padding
    override val height = rawHeight + 2 * padding
    override val horizontalAlign = HorizontalAlignment.LEFT
    override val verticalAlign = VerticalAlignment.TOP

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = drawPlayer(mouseOffsetX, mouseOffsetY)

    protected fun drawPlayer(mouseOffsetX: Int, mouseOffsetY: Int) {
        if (color != null) RenderLivingEntityHelper.setEntityColor(player, color, colorCondition)
        val mouse = Renderable.currentRenderPassMousePosition ?: return
        DrawContextUtils.pushPop {
            val peeked = DrawContextUtils.drawContext.pose().get(Matrix3x2f())
            val translationX = peeked.m20().toInt()
            val translationY = peeked.m21().toInt()
            val averageScale = (peeked.m00() + peeked.m11()) / 2
            val adjustedPadding = (padding * averageScale).toInt()
            val adjustedWidth = (width * averageScale).toInt()
            val adjustedHeight = (height * averageScale).toInt()
            // Expand scissor bounds to prevent head clipping during rotation.
            // Based on the rendered entity size rather than slot dimensions, since the
            // head can extend well beyond the slot when rotating at large entity scales.
            val headRoom = (entityScale * averageScale * 0.75).toInt()
            drawEntityWithoutScissor(
                DrawContextUtils.drawContext,
                adjustedPadding + translationX - headRoom,
                adjustedPadding + translationY - headRoom,
                adjustedPadding + adjustedWidth + translationX + headRoom,
                adjustedPadding + adjustedHeight + translationY + headRoom,
                (entityScale * averageScale).toInt(),
                0.0625f * averageScale,
                if (followMouse) (mouse.first - mouseOffsetX.toFloat()) * averageScale + translationX else eyesX,
                if (followMouse) (mouse.second - mouseOffsetY.toFloat()) * averageScale + translationY else eyesY,
                player,
            )
        }
    }
}

fun Renderable.Companion.fakePlayer(
    player: FakePlayer,
    followMouse: Boolean = false,
    eyesX: Float = 0f,
    eyesY: Float = 0f,
    width: Int = 50,
    height: Int = 100,
    entityScale: Int = 30,
    padding: Int = 5,
    color: Color? = null,
    colorCondition: () -> Boolean = { true },
) = FakePlayerRenderable(player, followMouse, eyesX, eyesY, entityScale, padding, color, colorCondition, width, height)

internal fun drawEntityWithoutScissor(
    guiGraphics: GuiGraphics,
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    size: Int,
    scale: Float,
    mouseX: Float,
    mouseY: Float,
    entity: LivingEntity,
) {
    //? if < 1.21.11 {
    val n: Float = (x1 + x2).toFloat() / 2.0f
    val o: Float = (y1 + y2).toFloat() / 2.0f
    val p = atan(((n - mouseX) / 40.0f).toDouble()).toFloat()
    val q = atan(((o - mouseY) / 40.0f).toDouble()).toFloat()
    val quaternionf = Quaternionf().rotateZ(3.1415927f)
    val quaternionf2 = Quaternionf().rotateX(q * 20.0f * 0.017453292f)
    quaternionf.mul(quaternionf2)
    val r: Float = entity.yBodyRot
    val s: Float = entity.yRot
    val t: Float = entity.xRot
    val u: Float = entity.yHeadRotO
    val v: Float = entity.yHeadRot
    entity.yBodyRot = 180.0f + p * 20.0f
    entity.yRot = 180.0f + p * 40.0f
    entity.xRot = -q * 20.0f
    entity.yHeadRot = entity.yRot
    entity.yHeadRotO = entity.yRot
    val w: Float = entity.scale
    val vector3f = Vector3f(0.0f, entity.bbHeight / 2.0f + scale * w, 0.0f)
    val x: Float = size.toFloat() / w
    InventoryScreen.renderEntityInInventory(guiGraphics, x1, y1, x2, y2, x, vector3f, quaternionf, quaternionf2, entity)
    entity.yBodyRot = r
    entity.yRot = s
    entity.xRot = t
    entity.yHeadRotO = u
    entity.yHeadRot = v
    //?} else
    //InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1, y1, x2, y2, size, scale, mouseX, mouseY, entity)
}
