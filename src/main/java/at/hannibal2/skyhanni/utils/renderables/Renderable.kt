package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import kotlin.math.max

interface Renderable {
    val width: Int
    fun isHovered(posX: Int, posY: Int) =
        Utils.getMouseX() in (posX..posX + width)
                && Utils.getMouseY() in (posY..posY + 10) // TODO: adjust for variable height?

    /**
     * N.B.: the offset is absolute, not relative to the position and should not be used for rendering
     * (the GL matrix stack should already be pre transformed)
     */
    fun render(posX: Int, posY: Int)

    companion object {
        fun fromAny(any: Any?, itemScale: Double = 1.0): Renderable? = when (any) {
            null -> placeholder(12)
            is Renderable -> any
            is String -> string(any)
            is ItemStack -> itemStack(any, itemScale)
            else -> null
        }

        fun link(text: String, onClick: () -> Unit): Renderable = link(string(text), onClick) { true }
        fun optionalLink(text: String, onClick: () -> Unit, condition: () -> Boolean = {true}): Renderable = link(string(text), onClick, condition)
        fun link(renderable: Renderable, onClick: () -> Unit, condition: () -> Boolean = {true}): Renderable {
            return clickable(hoverable(underlined(renderable), renderable, condition), onClick, 0, condition)
        }

        fun clickable(render: Renderable, onClick: () -> Unit, button: Int = 0, condition: () -> Boolean = {true}) = object : Renderable {
            override val width: Int
                get() = render.width

            private var wasDown = false

            override fun render(posX: Int, posY: Int) {
                val isDown = Mouse.isButtonDown(button)
                if (isDown > wasDown && isHovered(posX, posY)) {
                    if (condition()) {
                        onClick()
                    }
                }
                wasDown = isDown
                render.render(posX, posY)
            }

        }

        fun underlined(renderable: Renderable) = object : Renderable {
            override val width: Int
                get() = renderable.width

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 10, width, 11, 0xFFFFFFFF.toInt())
                GlStateManager.color(1F, 1F, 1F, 1F)
                renderable.render(posX, posY)
            }
        }

        fun hoverable(hovered: Renderable, unhovered: Renderable, condition: () -> Boolean = {true}) = object : Renderable {
            override val width: Int
                get() = max(hovered.width, unhovered.width)

            override fun render(posX: Int, posY: Int) {
                if (isHovered(posX, posY) && condition())
                    hovered.render(posX, posY)
                else
                    unhovered.render(posX, posY)
            }
        }

        fun itemStack(any: ItemStack, scale: Double = 1.0) = object : Renderable {
            override val width: Int
                get() = 12

            override fun render(posX: Int, posY: Int) {
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale)
            }
        }

        fun string(string: String) = object : Renderable {
            override val width: Int
                get() = Minecraft.getMinecraft().fontRendererObj.getStringWidth(string)

            override fun render(posX: Int, posY: Int) {
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("§f$string", 1f, 1f, 0)
            }
        }

        fun placeholder(width: Int) = object : Renderable {
            override val width: Int = width

            override fun render(posX: Int, posY: Int) {
            }
        }
    }
}