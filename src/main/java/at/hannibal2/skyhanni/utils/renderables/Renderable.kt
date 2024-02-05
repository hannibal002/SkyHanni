package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
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
            tips: List<String>,
            bypassChecks: Boolean = false,
            onClick: () -> Unit,
        ): Renderable {
            return clickable(hoverTips(text, tips, bypassChecks = bypassChecks), onClick, bypassChecks = bypassChecks)
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
            tips: List<String>,
            indexes: List<Int> = listOf(),
            stack: ItemStack? = null,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ): Renderable {

            val render = string(text)
            return object : Renderable {
                override val width = render.width
                override val height = render.height
                override val horizontalAlign = render.horizontalAlign
                override val verticalAlign = render.verticalAlign

                override fun render(posX: Int, posY: Int) {
                    render.render(posX, posY)
                    if (isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            list[Pair(posX, posY)] = indexes
                            GlStateManager.pushMatrix()
                            GlStateManager.translate(0F, 0F, 400F)

                            RenderLineTooltips.drawHoveringText(
                                posX, posY, tips,
                                stack,
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
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width: Int
                get() = 12
            override val height = 10
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                GlStateManager.pushMatrix()
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale)
                GlStateManager.popMatrix()
            }
        }

        fun singeltonString(string: String): List<Renderable> {
            return Collections.singletonList(string(string))
        }

        fun string(
            text: String,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {

            override val width: Int
                get() = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text)
            override val height = 10
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("§f$text", 1f, 1f, 0)
            }
        }

        fun placeholder(width: Int, height: Int = 10) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP

            override fun render(posX: Int, posY: Int) {
            }
        }
    }
}
