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
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard

class GuiPositionEditor(
    private val positions: List<Position>,
    private val border: Int,
    private val oldScreen: GuiContainer? = null,
) : SkyhanniBaseScreen() {

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
            val accessor = oldScreen as AccessorGuiContainer
            //#if MC < 1.21
            accessor.invokeDrawGuiContainerBackgroundLayer_skyhanni(partialTicks, -1, -1)
            //#else
            //$$ oldScreen.render(DrawContextUtils.drawContext, originalMouseX, originalMouseY, partialTicks)
            //#endif
        }

        GlStateManager.disableLighting()
        val hoveredPos = renderRectangles()

        renderLabels(hoveredPos)
    }

    private fun renderLabels(hoveredPos: Int) {
        var displayPos = -1
        if (clickedPos != -1 && positions[clickedPos].clicked) {
            displayPos = clickedPos
        }
        if (displayPos == -1) {
            displayPos = hoveredPos
        }

        // When the mouse isn't currently hovering over a gui element
        if (displayPos == -1) {
            val extraInfo = SkyHanniMod.feature.gui.keyBindOpen == Keyboard.KEY_NONE
            renderHover(
                buildList {
                    add("§cSkyHanni Position Editor")
                    if (extraInfo) {
                        add("§aTo edit hidden GUI elements set a key in /sh edit")
                        add("§athen click that key while the GUI element is visible")
                    }
                },
            )
            return
        }
        renderHover(getTextForPos(positions[displayPos]))
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
        )
    }

    private fun renderHover(text: List<String>) {
        RenderableTooltips.setTooltipForRender(text.map { Renderable.string(it) })
    }

    private fun renderRectangles(): Int {
        var hoveredPos = -1
        DrawContextUtils.pushMatrix()
        width = getScaledWidth()
        height = getScaledHeight()

        val (mouseX, mouseY) = GuiScreenUtils.mousePos

        var alreadyHadHover = false
        for ((index, position) in positions.withIndex().reversed()) {
            var elementWidth = position.getDummySize(true).x
            var elementHeight = position.getDummySize(true).y
            if (position.clicked) {
                grabbedX += position.moveX(mouseX - grabbedX, elementWidth)
                grabbedY += position.moveY(mouseY - grabbedY, elementHeight)
            }
            val x = position.getAbsX()
            val y = position.getAbsY()

            elementWidth = position.getDummySize().x
            elementHeight = position.getDummySize().y

            val isHovering = GuiRenderUtils.isPointInRect(
                mouseX,
                mouseY,
                x - border,
                y - border,
                elementWidth + border * 2,
                elementHeight + border * 2,
            ) && !alreadyHadHover

            val gray = -0x7fbfbfc0 // #40404080
            val selected = -0x7F0F0F10 // #F0F0F080
            GuiRenderUtils.drawRect(
                x - border,
                y - border,
                x + elementWidth + border * 2,
                y + elementHeight + border * 2,
                if (isHovering) selected else gray,
            )

            if (isHovering) {
                alreadyHadHover = true
                hoveredPos = index
            }
        }
        DrawContextUtils.popMatrix()
        return hoveredPos
    }

    private fun getScaledHeight() = GuiScreenUtils.scaledWindowHeight
    private fun getScaledWidth() = GuiScreenUtils.scaledWindowWidth

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos

        for (i in positions.indices.reversed()) {
            val position = positions[i]
            val elementWidth = position.getDummySize().x
            val elementHeight = position.getDummySize().y
            val x = position.getAbsX()
            val y = position.getAbsY()
            val isHovered = GuiRenderUtils.isPointInRect(
                mouseX,
                mouseY,
                x - border,
                y - border,
                elementWidth + border * 2,
                elementHeight + border * 2,
            )
            if (!isHovered) continue
            if (mouseButton == 1) {
                position.jumpToConfigOptions()
                break
            }
            if (!position.clicked && mouseButton == 0) {
                clickedPos = i
                position.clicked = true
                grabbedX = mouseX
                grabbedY = mouseY
                break
            }
        }
    }

    override fun onKeyTyped(typedChar: Char, keyCode: Int) {
        if (clickedPos == -1) return
        val position = positions[clickedPos]
        if (position.clicked) return

        val dist = if (KeyboardManager.isShiftKeyDown()) 10 else 1
        val elementWidth = position.getDummySize(true).x
        val elementHeight = position.getDummySize(true).y
        when (keyCode) {
            Keyboard.KEY_DOWN -> position.moveY(dist, elementHeight)
            Keyboard.KEY_UP -> position.moveY(-dist, elementHeight)
            Keyboard.KEY_LEFT -> position.moveX(-dist, elementWidth)
            Keyboard.KEY_RIGHT -> position.moveX(dist, elementWidth)
            Keyboard.KEY_MINUS -> position.scale -= .1F
            Keyboard.KEY_EQUALS -> position.scale += .1F
            Keyboard.KEY_SUBTRACT -> position.scale -= .1F
            Keyboard.KEY_ADD -> position.scale += .1F
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
        val mw = MouseCompat.getScrollDelta()
        if (mw == 0) return

        val (mouseX, mouseY) = GuiScreenUtils.mousePos

        val hovered = positions.firstOrNull { it.clicked }
            ?: positions.lastOrNull {
                val size = it.getDummySize()
                GuiRenderUtils.isPointInRect(
                    mouseX, mouseY,
                    it.getAbsX() - border, it.getAbsY() - border,
                    size.x + border * 2, size.y + border * 2,
                )
            } ?: return
        if (mw < 0)
            hovered.scale -= .1F
        else
            hovered.scale += .1F
    }
}
