package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import at.hannibal2.skyhanni.utils.guide.GuideGUI
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.calculateTableXOffsets
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.calculateTableYOffsets
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.MouseEvent
import org.lwjgl.input.Mouse
import java.awt.Color
import java.util.Collections
import kotlin.math.max

interface Renderable {

    val width: Int
    val height: Int

    val horizontalAlign: HorizontalAlignment
    val verticalAlign: VerticalAlignment
    fun isHovered(posX: Int, posY: Int) = currentRenderPassMousePosition?.let { (x, y) ->
        x in (posX .. posX + width)
            && y in (posY .. posY + height)
    } ?: false

    fun scrollInput(scrollOld: Float, posX: Int, posY: Int, button: Int?, minHeight: Int, maxHeight: Int, velocity: Double) =
        if (maxHeight < minHeight) minHeight.toFloat() else
            if (isHovered(posX, posY)) {
                var scroll = scrollOld
                if (button != null && Mouse.isButtonDown(button)) {
                    scroll += (Mouse.getEventDY() * velocity * 1/20).toFloat()
                    // LorenzDebug.log(Mouse.getEventDY().toString())
                }
                scroll += (Mouse.getEventDWheel() * velocity * 1 / (120 * 20)).toFloat()
                scroll.coerceIn(minHeight.toFloat(), maxHeight.toFloat())
            } else scrollOld

    /**
     * Pos x and pos y are relative to the mouse position.
     * (the GL matrix stack should already be pre transformed)
     */
    fun render(posX: Int, posY: Int)

    companion object {

        val logger = LorenzLogger("debug/renderable")
        val list = mutableMapOf<Pair<Int, Int>, List<Int>>()

        var currentRenderPassMousePosition: Pair<Int, Int>? = null
            set

        fun <T> withMousePosition(posX: Int, posY: Int, block: () -> T): T {
            val last = currentRenderPassMousePosition
            try {
                currentRenderPassMousePosition = Pair(posX, posY)
                return block()
            } finally {
                currentRenderPassMousePosition = last
            }
        }

        enum class HorizontalAlignment { Left, Center, Right }
        enum class VerticalAlignment { Top, Center, Bottom }

        fun fromAny(any: Any?, itemScale: Double = 1.0): Renderable? = when (any) {
            null -> placeholder(12)
            is Renderable -> any
            is String -> string(any)
            is ItemStack -> itemStack(any, itemScale)
            else -> null
        }

        fun link(text: String, bypassChecks: Boolean = false, onClick: () -> Unit): Renderable =
            link(string(text), onClick, bypassChecks = bypassChecks) { true }

        fun optionalLink(
            text: String,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ): Renderable =
            link(string(text), onClick, bypassChecks, condition)

        fun link(
            renderable: Renderable,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ): Renderable {
            return clickable(
                hoverable(underlined(renderable), renderable, bypassChecks, condition = condition),
                onClick,
                0,
                bypassChecks,
                condition
            )
        }

        fun clickAndHover(
            text: Any,
            tips: List<Any>,
            bypassChecks: Boolean = false,
            onClick: () -> Unit,
            onHover: () -> Unit = {},
        ): Renderable {
            return clickable(hoverTips(text, tips, bypassChecks = bypassChecks, onHover = onHover), onClick, bypassChecks = bypassChecks)
        }

        fun clickable(
            render: Renderable,
            onClick: () -> Unit,
            button: Int = 0,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ) =
            object : Renderable {
                override val width = render.width
                override val height = render.height
                override val horizontalAlign = render.horizontalAlign
                override val verticalAlign = render.verticalAlign

                private var wasDown = false

                override fun render(posX: Int, posY: Int) {
                    val isDown = Mouse.isButtonDown(button)
                    if (isDown > wasDown && isHovered(posX, posY) && condition() && shouldAllowLink(
                            true,
                            bypassChecks
                        )
                    ) {
                        onClick()
                    }
                    wasDown = isDown
                    render.render(posX, posY)
                }
            }

        fun hoverTips(
            text: Any,
            tips: List<Any>,
            indexes: List<Int> = listOf(),
            stack: ItemStack? = null,
            color: LorenzColor? = null,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            onHover: () -> Unit = {},
        ): Renderable {

            val render = fromAny(text) ?: string("Error")
            return object : Renderable {
                override val width = render.width
                override val height = render.height
                override val horizontalAlign = render.horizontalAlign
                override val verticalAlign = render.verticalAlign

                val tipsRender = tips.mapNotNull { fromAny(it) }

                override fun render(posX: Int, posY: Int) {
                    render.render(posX, posY)
                    if (isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            onHover.invoke()
                            list[Pair(posX, posY)] = indexes
                            GlStateManager.pushMatrix()
                            GlStateManager.translate(0F, 0F, 400F)

                            RenderLineTooltips.drawHoveringText(
                                posX,
                                posY,
                                tipsRender,
                                stack,
                                color,
                                currentRenderPassMousePosition?.first ?: Utils.getMouseX(),
                                currentRenderPassMousePosition?.second ?: Utils.getMouseY(),
                            )
                            GlStateManager.popMatrix()
                        }
                    } else {
                        if (list.contains(Pair(posX, posY))) {
                            list.remove(Pair(posX, posY))
                        }
                    }
                }
            }
        }

        private fun shouldAllowLink(debug: Boolean = false, bypassChecks: Boolean): Boolean {
            val guiScreen = Minecraft.getMinecraft().currentScreen

            val isGuiScreen = guiScreen != null
            if (bypassChecks) {
                return isGuiScreen
            }
            val isGuiPositionEditor = guiScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (guiScreen !is GuiEditSign && guiScreen !is GuideGUI<*>) {
                ToolTipData.lastSlot == null
            } else true
            val isConfigScreen = guiScreen !is GuiScreenElementWrapper

            val openGui = guiScreen?.javaClass?.name ?: "none"
            val isInNeuPv = openGui == "io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer"
            val isInSkyTilsPv = openGui == "gg.skytils.skytilsmod.gui.profile.ProfileGui"

            val result = isGuiScreen && isGuiPositionEditor && isNotInSignAndOnSlot && isConfigScreen &&
                !isInNeuPv && !isInSkyTilsPv

            if (debug) {
                if (!result) {
                    logger.log("")
                    logger.log("blocked link because:")
                    if (!isGuiScreen) logger.log("isGuiScreen")
                    if (!isGuiPositionEditor) logger.log("isGuiPositionEditor")
                    if (!isNotInSignAndOnSlot) logger.log("isNotInSignAndOnSlot")
                    if (!isConfigScreen) logger.log("isConfigScreen")
                    if (isInNeuPv) logger.log("isInNeuPv")
                    if (isInSkyTilsPv) logger.log("isInSkyTilsPv")
                    logger.log("")
                } else {
                    logger.log("allowed click")
                }
            }

            return result
        }

        fun underlined(renderable: Renderable) = object : Renderable {
            override val width: Int
                get() = renderable.width
            override val height = 10
            override val horizontalAlign = renderable.horizontalAlign
            override val verticalAlign = renderable.verticalAlign

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 10, width, 11, 0xFFFFFFFF.toInt())
                GlStateManager.color(1F, 1F, 1F, 1F)
                renderable.render(posX, posY)
            }
        }

        fun hoverable(
            hovered: Renderable,
            unhovered: Renderable,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ) =
            object : Renderable {
                override val width: Int
                    get() = max(hovered.width, unhovered.width)
                override val height = 10
                override val horizontalAlign get() = if (isHovered) hovered.horizontalAlign else unhovered.horizontalAlign
                override val verticalAlign get() = if (isHovered) hovered.verticalAlign else unhovered.verticalAlign

                var isHovered = false

                override fun render(posX: Int, posY: Int) {
                    if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks)) {
                        hovered.render(posX, posY)
                        isHovered = true
                    } else {
                        unhovered.render(posX, posY)
                        isHovered = false
                    }
                }
            }

        fun itemStack(
            any: ItemStack,
            scale: Double = 1.0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {
            override val width = (12 * scale).toInt()
            override val height = (10 * scale).toInt()
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                GlStateManager.pushMatrix()
                if (Minecraft.getMinecraft().currentScreen is GuiChat)
                    GlStateManager.translate(0F, 0F, -3F)
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale) // TODO fix the misalignment from the renderable Bounding Box (positon, width, height) to the render
                GlStateManager.popMatrix()
            }
        }

        fun singeltonString(string: String): List<Renderable> {
            return Collections.singletonList(string(string))
        }

        fun string(
            text: String,
            scale: Double = 1.0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {

            override val width = (Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) * scale).toInt() + 1
            override val height = (10.0 * scale).toInt() + 1
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val inverseScale = (1 / scale).toFloat()

            override fun render(posX: Int, posY: Int) {
                val fontRenderer = Minecraft.getMinecraft().fontRendererObj
                GlStateManager.scale(scale, scale, 1.0)
                fontRenderer.drawStringWithShadow("§f$text", inverseScale, inverseScale, 0)
                GlStateManager.scale(inverseScale, inverseScale, 1.0f)
            }
        }

        fun placeholder(width: Int, height: Int = 10) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = HorizontalAlignment.Left
            override val verticalAlign = VerticalAlignment.Top

            override fun render(posX: Int, posY: Int) {
            }
        }

        fun scrollList(
            list: List<Renderable>,
            height: Int,
            velocity: Double = 1.0,
            button: Int? = null,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {
            override val width = list.maxOf { it.width }
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight = list.maxOf { it.height }

            var scroll = 0f
            val scrollInt get() = scroll.toInt()

            private val end get() = scrollInt + height

            override fun render(posX: Int, posY: Int) {
                scroll = scrollInput(scroll, posX, posY, button, 0, virtualHeight - height, velocity)
                var renderY = 0
                var virtualY = 0
                list.forEach {
                    if (virtualY in scrollInt .. end) {
                        it.renderXAligned(posX, posY + renderY, width)
                        GlStateManager.translate(0f, it.height.toFloat(), 0f)
                        renderY += it.height
                    }
                    virtualY += it.height
                }
                GlStateManager.translate(0f, -renderY.toFloat(), 0f)
            }
        }

        fun scrollTable(
            content: List<List<Renderable?>>,
            height: Int,
            velocity: Double = 1.0,
            button: Int? = null,
            xPadding: Int = 1,
            yPadding: Int = 0,
            hasHeader: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {

            val xOffsets: List<Int> = calculateTableXOffsets(content, xPadding)
            val yOffsets: List<Int> = calculateTableYOffsets(content, yPadding)

            override val width = xOffsets.last() - xPadding
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight = yOffsets.last() - yPadding

            private val end get() = scrollInt + height - yPadding - 2 // TODO fix the -2 "fix"
            private val minHeight = if (hasHeader) yOffsets[1] else 0

            var scroll = minHeight.toFloat()

            val scrollInt get() = scroll.toInt()

            override fun render(posX: Int, posY: Int) {
                scroll = scrollInput(scroll, posX, posY, button, minHeight, virtualHeight - height, velocity)
                var renderY = 0
                if (hasHeader) {
                    content[0].forEachIndexed { index, renderable ->
                        GlStateManager.translate(xOffsets[index].toFloat(), 0f, 0f)
                        renderable?.renderXYAligned(posX + xOffsets[index], posY + renderY, xOffsets[index + 1] - xOffsets[index], yOffsets[1])
                        GlStateManager.translate(-xOffsets[index].toFloat(), 0f, 0f)
                    }
                    val yShift = yOffsets[1] - yOffsets[0]
                    GlStateManager.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }
                val range = yOffsets.indexOfFirst { it >= scrollInt } ..< (yOffsets.indexOfFirst { it >= end }.takeIf { it > 0 }
                    ?: (yOffsets.size - 1))
                for (rowIndex in range) {
                    content[rowIndex].forEachIndexed { index, renderable ->
                        GlStateManager.translate(xOffsets[index].toFloat(), 0f, 0f)
                        /* val buffer: FloatBuffer = ByteBuffer.allocateDirect(16 * java.lang.Float.BYTES)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer()
                        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer)
                        buffer.flip()
                        LorenzDebug.log(buffer[14].toString()) */

                        renderable?.renderXYAligned(posX + xOffsets[index], posY + renderY, xOffsets[index + 1] - xOffsets[index], yOffsets[rowIndex + 1] - yOffsets[rowIndex])
                        GlStateManager.translate(-xOffsets[index].toFloat(), 0f, 0f)
                    }
                    val yShift = yOffsets[rowIndex + 1] - yOffsets[rowIndex]
                    GlStateManager.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }
                GlStateManager.translate(0f, -renderY.toFloat(), 0f)
            }
        }

        /**
         * @param content the list of rows the table should render
         */
        fun table(
            content: List<List<Renderable?>>,
            xPadding: Int = 1,
            yPadding: Int = 0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {
            val xOffsets: List<Int> = calculateTableXOffsets(content, xPadding)
            val yOffsets: List<Int> = calculateTableYOffsets(content, yPadding)
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override val width = xOffsets.last() - xPadding
            override val height = yOffsets.last() - yPadding

            override fun render(posX: Int, posY: Int) {
                content.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { index, renderable ->
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(xOffsets[index].toFloat(), yOffsets[rowIndex].toFloat(), 0F)
                        renderable?.renderXYAligned(posX + xOffsets[index], posY + yOffsets[rowIndex], xOffsets[index + 1] - xOffsets[index], yOffsets[rowIndex + 1] - yOffsets[rowIndex])
                        GlStateManager.popMatrix()
                    }
                }
            }
        }

        fun progressBar(
            percent: Double,
            startColor: Color = Color(255, 0, 0),
            endColor: Color = Color(0, 255, 0),
            width: Int = 30,
            height: Int = 4,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val progress = (1.0 + percent * (width - 2.0)).toInt()
            val color = ColorUtils.blend(startColor, endColor, percent)

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 0, width, height, 0xFF43464B.toInt())
                Gui.drawRect(1, 1, width - 1, height - 1, color.darker().rgb)
                Gui.drawRect(1, 1, progress, height - 1, color.rgb)
            }
        }

        fun wrappedString(
            text: String,
            width: Int,
            scale: Double = 1.0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {

            val list = Minecraft.getMinecraft().fontRendererObj.listFormattedStringToWidth(text, (width / scale).toInt())

            override val width = width
            override val height = (list.size * 10 * scale).toInt()
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val inverseScale = (1 / scale).toFloat()

            override fun render(posX: Int, posY: Int) {
                val fontRenderer = Minecraft.getMinecraft().fontRendererObj
                fontRenderer.drawString("§f", 0, 0, 0)
                GlStateManager.scale(scale, scale, 1.0)
                list.forEachIndexed { index, text ->
                    fontRenderer.drawStringWithShadow(text, inverseScale, inverseScale + index * 10.0f, 0)
                }
                GlStateManager.scale(inverseScale, inverseScale, 1.0f)
            }
        }

        fun calculateStretchXPadding(content: List<List<Renderable?>>, xSpace: Int): Int {
            if (content.isEmpty()) return xSpace
            val xWidth = content.maxOf { it.sumOf { it?.width ?: 0 } }
            val xLength = content.maxOf { it.size }
            val emptySpace = xSpace - xWidth
            if (emptySpace < 0) {
//                throw IllegalArgumentException("Not enough space for content")
            }
            return emptySpace / xLength
        }

        fun itemStackWithTip(
            item: ItemStack,
            scale: Double = 1.0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) =
            hoverTips(itemStack(item, scale, horizontalAlign, verticalAlign), item.getTooltip(Minecraft.getMinecraft().thePlayer, false), stack = item)

        fun leftRightBox(
            // TODO find a more general solution
            left: Renderable,
            right: Renderable,
            width: Int,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {
            override val width = width
            override val height = max(left.height, right.height)
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val rightOffset = width - right.width

            override fun render(posX: Int, posY: Int) {
                left.render(posX, posY)
                GlStateManager.translate(rightOffset.toFloat(), 0f, 0f)
                right.render(posX + rightOffset, posY)
                GlStateManager.translate(-rightOffset.toFloat(), 0f, 0f)
            }
        }

        fun verticalList(
            list: List<Renderable>,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.Left,
            verticalAlign: VerticalAlignment = VerticalAlignment.Top,
        ) = object : Renderable {
            override val width = list.maxOf { it.width }
            override val height = list.maxOf { it.height }
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                var yOffset = 0
                list.forEach {
                    it.render(posX, posY + yOffset)
                    val yShift = it.height
                    GlStateManager.translate(0f, yShift.toFloat(), 0f)
                    yOffset += yShift
                }
                GlStateManager.translate(0f, -yOffset.toFloat(), 0f)
            }
        }
    }
}
