/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */
package at.hannibal2.skyhanni.config.core.config.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.getDummySize
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.utils.render.ModernGlStateManager
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

class GuiPositionEditor(
    private val positions: List<Position>,
    private val border: Int,
    private val oldScreen: SkyHanniGuiContainer? = null,
) : SkyHanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.gui
    private var grabbedX = 0
    private var grabbedY = 0
    private var clickedPos = -1

    override fun guiClosed() {
        clickedPos = -1
        for (position in positions) {
            position.clicked = false
        }
        OtherInventoryData.close()
    }

    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        // Items aren't drawn due to a bug in neu rendering
        drawDefaultBackground(originalMouseX, originalMouseY, partialTicks)
        if (oldScreen != null) {
            //#if MC > 1.21.5
            //$$ oldScreen.renderBg(DrawContextUtils.drawContext, partialTicks, originalMouseX, originalMouseY)
            //#endif
            oldScreen.render(DrawContextUtils.drawContext, originalMouseX, originalMouseY, partialTicks)
        }

        ModernGlStateManager.disableLighting()
        val hoveredPos = renderRectangles()

        renderLabels(hoveredPos)
    }

    private fun renderLabels(hoveredPos: Int) {
        val displayPos = when {
            clickedPos != -1 && positions[clickedPos].clicked -> clickedPos
            else -> hoveredPos
        }

        // When the mouse isn't currently hovering over a gui element
        val text = if (displayPos == -1) {
            val extraInfo = SkyHanniMod.feature.gui.keyBindOpen == GLFW.GLFW_KEY_UNKNOWN

            buildList {
                add("§cSkyHanni Position Editor")
                if (extraInfo) {
                    add("§aTo edit hidden GUI elements set a key in /sh edit")
                    add("§athen click that key while the GUI element is visible")
                }
            }
        } else {
            getTextForPos(positions[displayPos])
        }

        renderHover(text)
    }

    private fun getTextForPos(pos: Position): List<String> {
        if (pos.clicked) return listOf("§7x: §e${pos.x}§7, y: §e${pos.y}")

        return listOf(
            "§cSkyHanni Position Editor",
            "§b${pos.internalName}",
            "  §7x: §e${pos.x}§7, y: §e${pos.y}§7, scale: §e${pos.scale.roundTo(2)}",
            "",
            "§eRight-Click to open associated config options!",
            "§eUse Scroll-Wheel to resize!",
            "§e${KeyboardManager.getKeyName(config.keyBindReset)} to reset to default position!",
        )
    }

    private fun renderHover(text: List<String>) {
        RenderableTooltips.setTooltipForRender(text.map(StringRenderable::from))
    }

    private fun renderRectangles(): Int {
        var hoveredPos = -1
        DrawContextUtils.pushPop {
            width = getScaledWidth()
            height = getScaledHeight()

            val (mouseX, mouseY) = GuiScreenUtils.mousePos
            var alreadyHadHover = false

            for ((index, position) in positions.withIndex().reversed()) {
                val dummy = position.getDummySize(true)
                if (position.clicked) {
                    grabbedX += position.moveX(mouseX - grabbedX, dummy.x)
                    grabbedY += position.moveY(mouseY - grabbedY, dummy.y)
                }

                val isHovering = position.isHovered() && !alreadyHadHover

                val x = position.getAbsX()
                val y = position.getAbsY()

                val gray = -0x7fbfbfc0 // #40404080
                val selected = -0x7F0F0F10 // #F0F0F080
                GuiRenderUtils.drawRect(
                    x - border,
                    y - border,
                    x + position.getDummySize().x + border * 2,
                    y + position.getDummySize().y + border * 2,
                    if (isHovering) selected else gray,
                )

                if (isHovering) {
                    alreadyHadHover = true
                    hoveredPos = index
                }
            }
        }

        return hoveredPos
    }

    private fun getScaledHeight() = GuiScreenUtils.scaledWindowHeight
    private fun getScaledWidth() = GuiScreenUtils.scaledWindowWidth

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos

        for (i in positions.indices.reversed()) {
            val position = positions[i]
            if (!position.isHovered()) continue

            when (mouseButton) {
                1 -> position.jumpToConfigOptions()
                2 -> if (config.keyBindReset == KeyboardManager.MIDDLE_MOUSE) position.resetPositionAndScale()
                0 -> if (!position.clicked) {
                    clickedPos = i
                    position.clicked = true
                    grabbedX = mouseX
                    grabbedY = mouseY
                }
            }

            break
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        if (keyCode == config.keyBindReset) {
            positions.firstOrNull { it.isHovered() }?.resetPositionAndScale()
            return
        }
        if (clickedPos == -1) return
        val position = positions[clickedPos]
        if (position.clicked) return

        val dist = if (KeyboardManager.isShiftKeyDown()) 10 else 1
        val elementWidth = position.getDummySize(true).x
        val elementHeight = position.getDummySize(true).y
        when (keyCode) {
            GLFW.GLFW_KEY_DOWN -> position.moveY(dist, elementHeight)
            GLFW.GLFW_KEY_UP -> position.moveY(-dist, elementHeight)
            GLFW.GLFW_KEY_LEFT -> position.moveX(-dist, elementWidth)
            GLFW.GLFW_KEY_RIGHT -> position.moveX(dist, elementWidth)
            GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> position.scale -= .1F
            GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> position.scale += .1F
        }
    }

    private fun Position.isHovered(): Boolean {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos

        val elementWidth = getDummySize().x
        val elementHeight = getDummySize().y
        val x = getAbsX()
        val y = getAbsY()
        return GuiRenderUtils.isPointInRect(
            mouseX,
            mouseY,
            x - border,
            y - border,
            elementWidth + border * 2,
            elementHeight + border * 2,
        )
    }

    private fun Position.resetPositionAndScale() {
        val field = linkField ?: return
        val clazz = field.declaringClass.kotlin
        val instance = clazz.createInstance()

        val defaultPosition = clazz.declaredMemberProperties
            .firstNotNullOfOrNull { property ->
                property.javaField
                    ?.getAnnotation(ConfigLink::class.java)
                    ?.takeIf { it.field == field.name }
                    ?.let { property.getter.call(instance) as? Position }
            } ?: return

        with(this) {
            moveTo(defaultPosition.x, defaultPosition.y)
            scale = defaultPosition.scale
        }
    }

    override fun onMouseReleased(originalMouseX: Int, originalMouseY: Int, state: Int) {
        for (position in positions) {
            position.clicked = false
        }
    }

    override fun onMouseClickMove(originalMouseX: Int, originalMouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        for (position in positions) {
            if (!position.clicked) continue

            val (mouseX, mouseY) = GuiScreenUtils.mousePos

            val elementWidth = position.getDummySize(true).x
            val elementHeight = position.getDummySize(true).y
            grabbedX += position.moveX(mouseX - grabbedX, elementWidth)
            grabbedY += position.moveY(mouseY - grabbedY, elementHeight)
            GuiEditManager.handleGuiPositionMoved(position.internalName ?: continue)
        }
    }

    override fun onHandleMouseInput() {
        val scroll = MouseCompat.getScrollDelta().takeIf { it != 0 } ?: return
        val hovered = positions.firstOrNull { it.clicked } ?: positions.lastOrNull { it.isHovered() } ?: return
        hovered.scale += if (scroll > 0) .1F else -.1F
    }

    override fun onClose() {
        if (oldScreen == null) {
            super.onClose()
        } else {
            Minecraft.getInstance().screen = oldScreen
        }
    }
}
