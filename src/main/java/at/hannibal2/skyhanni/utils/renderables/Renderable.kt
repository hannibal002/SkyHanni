package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
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
        x in (posX..posX + width)
            && y in (posY..posY + height) // TODO: adjust for variable height?
    } ?: false

    fun scrollInput(scrollOld: Int, posX: Int, posY: Int, button: Int?, minHeight: Int, maxHeight: Int, velocity: Double) =
        if (isHovered(posX, posY)) {
            var scroll = scrollOld
            if (button != null && Mouse.isButtonDown(button)) {
                scroll += (Mouse.getEventDY() * 0.5 * velocity).toInt()
                // LorenzDebug.log(Mouse.getEventDY().toString())
            }
            scroll += (Mouse.getEventDWheel() * velocity * 0.02).toInt()

            when {
                scroll > maxHeight -> maxHeight
                scroll < minHeight -> minHeight
                else -> scroll
            }
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
            text: String,
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
            text: String,
            tips: List<Any>,
            indexes: List<Int> = listOf(),
            stack: ItemStack? = null,
            color: LorenzColor? = null,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            onHover: () -> Unit = {},
        ): Renderable {

            val render = string(text)
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
            val isGuiScreen = Minecraft.getMinecraft().currentScreen != null
            if (bypassChecks) {
                return isGuiScreen
            }
            val isGuiPositionEditor = Minecraft.getMinecraft().currentScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (Minecraft.getMinecraft().currentScreen !is GuiEditSign) {
                ToolTipData.lastSlot == null
            } else true
            val isConfigScreen = Minecraft.getMinecraft().currentScreen !is GuiScreenElementWrapper

            val openGui = Minecraft.getMinecraft().currentScreen?.javaClass?.name ?: "none"
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
            override val width: Int
                get() = 12
            override val height = 10
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                GlStateManager.pushMatrix()
                if (Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen is GuiChat)
                    GlStateManager.translate(0F, 0F, -145F)
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale)
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

            override fun render(posX: Int, posY: Int) {
            }
        }

        /**
         * @param content the list of rows the table should render
         */
        fun table(content: List<List<Renderable?>>, xPadding: Int = 1, yPadding: Int = 0) = object : Renderable {
            val xOffsets: List<Int> = let {
                var buffer = 0
                var index = 0
                buildList {
                    add(0)
                    while (true) {
                        buffer += content.map { it.getOrNull(index) }.takeIf { it.any { it != null } }?.maxOf {
                            it?.width ?: 0
                        }?.let { it + xPadding } ?: break
                        add(buffer)
                        index++
                    }
                }
            }
            val yOffsets: List<Int> = let {
                var buffer = 0
                listOf(0) + content.map { row ->
                    buffer += row.maxOf { it?.height ?: 0 } + yPadding
                    buffer
                }
            }

            override val width = xOffsets.last() - xPadding
            override val height = yOffsets.last() - yPadding

            override fun render(posX: Int, posY: Int) {
                content.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { index, renderable ->
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(xOffsets[index].toFloat(), yOffsets[rowIndex].toFloat(), 0F)
                        renderable?.render(posX + xOffsets[index], posY + yOffsets[rowIndex])
                        GlStateManager.popMatrix()
                    }
                }
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

            var scroll = 0

            private val end get() = scroll + height

            override fun render(posX: Int, posY: Int) {
                scroll = scrollInput(scroll, posX, posY, button, 0, virtualHeight - height, velocity)
                var renderY = 0
                var virtualY = 0
                list.forEach {
                    if (virtualY in scroll..end) {
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

            private val end get() = scroll + height - yPadding - 2 // TODO fix the -2 "fix"
            private val minHeight = if (hasHeader) yOffsets[1] else 0

            var scroll = minHeight

            override fun render(posX: Int, posY: Int) {
                scroll = if (virtualHeight > height)
                    scrollInput(scroll, posX, posY, button, minHeight, virtualHeight - height, velocity)
                else minHeight
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
                val range = yOffsets.indexOfFirst { it >= scroll }..<(yOffsets.indexOfFirst { it >= end }.takeIf { it > 0 }
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

    }
}
