package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isRancherSign
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class GuiEditManager {

    private var lastHotkeyPressed = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!LorenzUtils.onHypixel) return
        if (event.keyCode != SkyHanniMod.feature.gui.keyBindOpen) return
        if (isInGui()) return

        Minecraft.getMinecraft().currentScreen?.let {
            if (it !is GuiInventory && it !is GuiChest && it !is GuiEditSign && it !is GuiProfileViewer) return
            if (it is GuiEditSign && !it.isRancherSign()) return
        }

        if (lastHotkeyPressed.passedSince() < 500.milliseconds) return
        if (NEUItems.neuHasFocus()) return
        lastHotkeyPressed = SimpleTimeMark.now()

        openGuiPositionEditor(hotkeyReminder = false)
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

        private var lastHotkeyReminded = SimpleTimeMark.farPast()

        @JvmStatic
        fun openGuiPositionEditor(hotkeyReminder: Boolean) {
            SkyHanniMod.screenToOpen = GuiPositionEditor(latestPositions.values.toList(), 2)
            if (hotkeyReminder && lastHotkeyReminded.passedSince() > 30.minutes) {
                lastHotkeyReminded = SimpleTimeMark.now()
                LorenzUtils.chat(
                    "§eTo edit hidden GUI elements:\n" +
                            " §7- §e1. Set a key in /sh edit.\n" +
                            " §7- §e2. Click that key while the GUI element is visible."
                )
            }
        }

        @JvmStatic
        fun renderLast() {
            if (!isInGui()) return
            if (!SkyHanniDebugsAndTests.globalRender) return

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
