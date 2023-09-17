package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isRancherSign
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.util.*

class GuiEditManager {

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        Minecraft.getMinecraft().currentScreen?.let {
            if (it !is GuiInventory && it !is GuiChest && it !is GuiEditSign) return
        }

        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (SkyHanniMod.feature.gui.keyBindOpen != key) return

        if (NEUItems.neuHasFocus()) return

        val screen = Minecraft.getMinecraft().currentScreen
        if (screen is GuiEditSign) {
            if (!screen.isRancherSign()) return
        }

        if (isInGui()) return
        openGuiPositionEditor()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        latestPositions = currentPositions.toMap()
        currentPositions.clear()
    }

    companion object {
        var currentPositions = mutableMapOf<String, Position>()
        private var latestPositions = mapOf<String, Position>()
        private var currentBorderSize = mutableMapOf<String, Pair<Int, Int>>()

        @JvmStatic
        fun add(position: Position, posLabel: String, x: Int, y: Int) {
            var name = position.internalName
            if (name == null) {
                name = if (posLabel == "none") "none " + UUID.randomUUID() else posLabel
                position.internalName = name
            }
            if (!currentPositions.containsKey(name)) {
                currentPositions[name] = position
                currentBorderSize[posLabel] = Pair(x, y)
            }
        }

        @JvmStatic
        fun openGuiPositionEditor() {
            SkyHanniMod.screenToOpen = GuiPositionEditor(latestPositions.values.toList(), 2)
        }

        @JvmStatic
        fun renderLast() {
            if (!isInGui()) return

            GlStateManager.translate(0f, 0f, 200f)

            GuiRenderEvent.GuiOverlayRenderEvent().postAndCatch()

            GlStateManager.pushMatrix()
            GlStateManager.enableDepth()
            GuiRenderEvent.ChestGuiOverlayRenderEvent().postAndCatch()
            GlStateManager.popMatrix()

            GlStateManager.translate(0f, 0f, -200f)
        }

        fun isInGui() = Minecraft.getMinecraft().currentScreen is GuiPositionEditor

        fun Position.getDummySize(random: Boolean = false): Vector2i {
            if (random) return Vector2i(5, 5)
            val (x, y) = currentBorderSize[internalName] ?: return Vector2i(1, 1)
            return Vector2i((x * effectiveScale).toInt(), (y * effectiveScale).toInt())
        }

        fun Position.getAbsX() = getAbsX0(getDummySize(true).x)

        fun Position.getAbsY() = getAbsY0(getDummySize(true).y)
    }
}

class Vector2i(val x: Int, val y: Int)