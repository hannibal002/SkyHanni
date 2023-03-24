package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.Vector2f
import java.util.*

class GuiEditManager {

    // TODO Make utils method for this
    @SubscribeEvent
    fun onKeyBindPressed(event: InputEvent.KeyInputEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (SkyHanniMod.feature.gui.keyBindOpen == key) {
            if (isInGui()) return
            openGuiEditor()
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderOverlay(event: GuiRenderEvent) {
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
        fun openGuiEditor() {
            val savedGui = Minecraft.getMinecraft().currentScreen

            val help = LinkedHashMap<Position, Position>()
            for (position in latestPositions.values) {
                help[position] = position
            }

            val guiPositionEditor = GuiPositionEditor(help) { NotEnoughUpdates.INSTANCE.openGui = savedGui }
            SkyHanniMod.screenToOpen = guiPositionEditor
        }

        @JvmStatic
        fun renderLast() {
            if (!isInGui()) return

            GlStateManager.translate(0f, 0f, 200f)

            GuiRenderEvent.GameOverlayRenderEvent().postAndCatch()

            GlStateManager.pushMatrix()
            GlStateManager.enableDepth()
            GuiRenderEvent.ChestBackgroundRenderEvent().postAndCatch()
            GlStateManager.popMatrix()

            GlStateManager.translate(0f, 0f, -200f)
        }

        fun isInGui() = Minecraft.getMinecraft().currentScreen is GuiPositionEditor

        fun Position.getDummySize(random: Boolean = false): Vector2f {
            if (random) {
                return Vector2f(15f, 15f)
            } else {
                val (x, y) = currentBorderSize[internalName] ?: return Vector2f(1f, 1f)
                return Vector2f(x.toFloat(), y.toFloat())
            }
        }

        fun Position.getAbsX_(): Int {
            val width = getDummySize(true).x
            return getAbsX(ScaledResolution(Minecraft.getMinecraft()), width.toInt())
        }

        fun Position.getAbsY_(): Int {
            val height = getDummySize(true).y
            return getAbsY(ScaledResolution(Minecraft.getMinecraft()), height.toInt())
        }

        fun Position.isCenterX_(): Boolean {
            return false
        }
    }
}