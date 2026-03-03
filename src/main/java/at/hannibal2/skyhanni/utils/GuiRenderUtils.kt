package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemBlink.checkBlinkItem
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.RenderCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.text.DecimalFormat
import kotlin.math.min
import kotlin.math.sqrt
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.world.item.ItemDisplayContext

/**
 * Some functions taken from NotEnoughUpdates
 */
@Suppress("UnusedParameter")
object GuiRenderUtils {

    private val fr: Font get() = Minecraft.getInstance().font

    private fun drawStringCentered(str: String, x: Float, y: Float, shadow: Boolean, color: Int) {
        val strLen = fr.width(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.lineHeight / 2f
        DrawContextUtils.drawContext.drawString(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    }

    private fun drawStringCentered(str: Component, x: Float, y: Float, shadow: Boolean, color: Int) {
        val strLen = fr.width(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.lineHeight / 2f
        DrawContextUtils.drawContext.drawString(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    }

    fun drawStringCentered(str: String, x: Int, y: Int) {
        drawStringCentered(str, x.toFloat(), y.toFloat(), true, -1)
    }

    fun drawStringCentered(str: Component, x: Int, y: Int) {
        drawStringCentered(str, x.toFloat(), y.toFloat(), true, -1)
    }

    fun drawStringCenteredScaledMaxWidth(text: String, x: Float, y: Float, shadow: Boolean, length: Int, color: Int) {
        DrawContextUtils.pushMatrix()
        val strLength = fr.width(text)
        val factor = min((length / strLength.toFloat()).toDouble(), 1.0).toFloat()
        DrawContextUtils.translate(x, y)
        DrawContextUtils.scale(factor, factor)
        drawString(text, -strLength / 2, -fr.lineHeight / 2, color, shadow)
        DrawContextUtils.popMatrix()
    }

    fun drawString(str: String, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: String, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    fun drawString(str: Component, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: Component, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    fun drawString(str: FormattedCharSequence, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: FormattedCharSequence, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    fun drawStrings(strings: String, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        drawStrings(strings.split("\n"), x, y, color, shadow)
    }

    fun drawStrings(strings: List<String>, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        var newY = y
        for (string in strings) {
            DrawContextUtils.drawContext.drawString(fr, string, x, newY, color, shadow)
            newY += 9
        }
    }

    fun drawTexts(strings: List<Component>, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        var newY = y
        for (string in strings) {
            DrawContextUtils.drawContext.drawString(fr, string, x, newY, color, shadow)
            newY += 9
        }
    }

    fun isPointInRect(x: Int, y: Int, left: Int, top: Int, width: Int, height: Int) =
        left <= x && x < left + width && top <= y && y < top + height

    fun getFarmingBar(
        label: String,
        tooltip: String,
        currentValue: Number,
        maxValue: Number,
        width: Int,
        textScale: Float = .7f,
    ): Renderable {
        val current = currentValue.toDouble().coerceAtLeast(0.0)
        val percent = current.fractionOf(maxValue)
        val scale = textScale.toDouble()
        return with(Renderable) {
            hoverTips(
                vertical(
                    text(label, scale = scale),
                    fixedSizeLine(
                        listOf(
                            text(
                                "§2${DecimalFormat("0.##").format(current)} / ${
                                    DecimalFormat(
                                        "0.##",
                                    ).format(maxValue)
                                }☘",
                                scale = scale, horizontalAlign = HorizontalAlignment.LEFT,
                            ),
                            text(
                                "§2${(percent * 100).roundTo(1)}%",
                                scale = scale,
                                horizontalAlign = HorizontalAlignment.RIGHT,
                            ),
                        ),
                        width,
                    ),
                    progressBar(percent, width = width),
                ),
                tooltip.split('\n').map(StringRenderable::from),
            )
        }
    }

    fun drawScaledRec(left: Int, top: Int, right: Int, bottom: Int, color: Int, inverseScale: Float) {
        drawRect(
            (left * inverseScale).toInt(),
            (top * inverseScale).toInt(),
            (right * inverseScale).toInt(),
            (bottom * inverseScale).toInt(),
            color,
        )
    }

    fun drawRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        DrawContextUtils.drawContext.fill(left, top, right, bottom, color)
    }

    fun renderItemAndBackground(item: ItemStack, x: Int, y: Int, color: Int) {
        DrawContextUtils.drawItem(item, x, y)
        drawRect(x, y, x + 16, y + 16, color)
    }

    fun drawGradientRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        startColor: Int = -0xfeffff0,
        endColor: Int = -0xfeffff0,
        zLevel: Double = 0.0,
    ) {
        DrawContextUtils.drawContext.fillGradient(left, top, right, bottom, startColor, endColor)
    }

    fun drawTexturedRect(x: Float, y: Float, texture: Identifier, alpha: Float = 1f) {
        val drawContext = DrawContextUtils.drawContext
        drawTexturedRect(
            x,
            y,
            drawContext.guiWidth().toFloat(),
            drawContext.guiHeight().toFloat(),
            texture = texture,
            alpha = alpha,
        )
    }

    fun drawTexturedRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        uMin: Float = 0f,
        uMax: Float = width.toFloat(),
        vMin: Float = 0f,
        vMax: Float = height.toFloat(),
        texture: Identifier,
        alpha: Float = 1f,
    ) {
        drawTexturedRect(
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            uMin,
            uMax,
            vMin,
            vMax,
            texture,
            alpha,
        )
    }

    private fun drawTexturedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        uMin: Float = 0f,
        uMax: Float = width,
        vMin: Float = 0f,
        vMax: Float = height,
        texture: Identifier,
        alpha: Float = 1f,
    ) {
        DrawContextUtils.drawContext.blit(
            RenderCompat.getMinecraftGuiTextured(),
            texture,
            x.toInt(),
            y.toInt(),
            uMin,
            vMin,
            uMax.toInt(),
            vMax.toInt(),
            width.toInt(),
            height.toInt(),
            ARGB.white(alpha)
        )
    }

    fun enableScissor(left: Int, top: Int, right: Int, bottom: Int) {
        DrawContextUtils.drawContext.enableScissor(left, top, right, bottom)
    }

    fun disableScissor() {
        DrawContextUtils.drawContext.disableScissor()
    }

    private fun drawFloatingRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        light: Int = -0xcfcfca,
        dark: Int = -0xefefea,
        shadow: Boolean = true,
    ) {
        val alpha = -0x1000000

        val main = alpha or 0x202026
        drawRect(x, y, x + 1, y + height, light) // Left
        drawRect(x + 1, y, x + width, y + 1, light) // Top1
        drawRect(x + width - 1, y + 1, x + width, y + height, dark) // Right
        drawRect(x + 1, y + height - 1, x + width - 1, y + height, dark) // Bottom
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, main) // Middle
        if (shadow) {
            drawRect(x + width, y + 2, x + width + 2, y + height + 2, 0x70000000) // Right shadow
            drawRect(x + 2, y + height, x + width, y + height + 2, 0x70000000) // Bottom shadow
        }
    }

    fun drawFloatingRectDark(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        shadow: Boolean = true,
    ) {
        drawFloatingRect(
            x,
            y,
            width,
            height,
            -0xcfcfca,
            -0xefefea,
            shadow = shadow,
        )
    }

    fun drawFloatingRectLight(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        shadow: Boolean = true,
    ) {
        drawFloatingRect(
            x,
            y,
            width,
            height,
            light = -0xefefea,
            dark = -0xcfcfca,
            shadow = shadow,
        )
    }

    private const val SKULL_SCALE = (5f / 4f)

    /**
     * Returns either the stable ID of the custom render (if used) or -1 if the item was
     * rendered using the normal method (either is static, or 'small')
     */
    @Suppress("unused")
    fun ItemStack.renderOnScreen(
        x: Float,
        y: Float,
        scale: Double = NeuItems.ITEM_FONT_SIZE,
        rescaleSkulls: Boolean = true,
        rotationVec: Vec3 = Vec3.ZERO,
        translationVec: Vec3 = Vec3.ZERO,
        stableRenderId: Int? = null,
    ): Int {
        val item = checkBlinkItem()
        val isItemSkull = rescaleSkulls && item.isSkull()

        val baseItemScale = if (isItemSkull) SKULL_SCALE else 1f
        val finalItemScale = (baseItemScale * scale)

        val (translateX, translateY) = if (isItemSkull) {
            val skullDiff = (scale.toFloat() * 2.5f)
            x - skullDiff to y - skullDiff
        } else x to y

        val matrices2D = DrawContextUtils.drawContext.pose()

        // And similarly, we need to extract the scaling from the GUI editor as well, since we're building our own stack.
        val guiScaleX = sqrt(matrices2D.m00() * matrices2D.m00() + matrices2D.m01() * matrices2D.m01())
        val guiScaleY = sqrt(matrices2D.m10() * matrices2D.m10() + matrices2D.m11() * matrices2D.m11())
        val totalItemScale = ((guiScaleX + guiScaleY) * 0.5f) * finalItemScale

        // Check if the model uses 3D lighting
        val trackingState = TrackingItemStackRenderState()
        Minecraft.getInstance().itemModelResolver.updateForTopItem(trackingState, item, ItemDisplayContext.GUI, null, null, 0)

        // Todo, uncomment when modern item rendering is fixed
        /* if (rotationVec == Vec3.ZERO && (totalItemScale <= 1 || !trackingState.usesBlockLight())) */
        return item.normalRenderOnScreen(translateX, translateY, finalItemScale.toFloat())

        /*
        /**
         * This is used to render items that fit these criteria:
         *  - Uses block light (mostly skulls)
         *  - Has a rotation (either from the GUI editor or from the animation)
         *  - Has animations (either animated skins/items, or a bounce animation from AnimatedItemStackRenderable)
         *  - Needs to be rendered at a higher resolution (scale > 1)
         *
         *  Any place that this function is called (I.e., from calling .render() on an AnimatedItemStackRenderable),
         *  we _MUST_ do so from a GameOverlayRenderPostEvent. If an item is rendered in a GuiRenderEvent with this logic,
         *  the item will render correctly, but will end up "on top" of almost all other GUI elements, including our own config,
         *  and will not correctly adhere to other GUI transforms (such as blurring when in a menu).
         */
        val guiRenderState = GuiItemRenderState(
            this.item.name.toString(),
            Matrix3x2f(DrawContextUtils.drawContext.pose()),
            trackingState,
            0,
            0,
            DrawContextUtils.drawContext.scissorStack.peek()
        )
        val newRenderState = SkyHanniGuiItemRenderState(
            guiRenderState, x, y,
            rotationVec, translationVec,
            scale = scale.toFloat(),
            adjustedScale = (scale * guiScaleX).toFloat(),
            stableRenderId,
        )
        Minecraft.getInstance().gameRenderer.guiRenderState.submitPicturesInPictureState(newRenderState)
        return newRenderState.stableId
        */
    }

    private fun ItemStack.normalRenderOnScreen(
        translateX: Float,
        translateY: Float,
        scale: Float
    ): Int = DrawContextUtils.pushPopResult {
        DrawContextUtils.translate(translateX, translateY)
        DrawContextUtils.scale(scale, scale)

        RenderSystem.assertOnRenderThread()

        Minecraft.getInstance().gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)

        DrawContextUtils.drawItem(this, 0, 0)
        return@pushPopResult -1
    }
}
