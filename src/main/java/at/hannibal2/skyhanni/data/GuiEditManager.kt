package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.events.GuiPositionMovedEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SignUtils.isGardenSign
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GuiEditManager {

    private var lastHotkeyPressed = SimpleTimeMark.farPast()

    private val currentPositions = TimeLimitedCache<String, Position>(15.seconds)
    private val currentBorderSize = mutableMapOf<String, Pair<Int, Int>>()
    private var lastMovedGui: String? = null

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (event.keyCode != SkyHanniMod.feature.gui.keyBindOpen) return
        if (event.keyCode == GLFW.GLFW_KEY_ENTER) {
            ChatUtils.chat("You can't use Enter as a keybind to open the gui editor!")
            return
        }
        if (isInGui()) return

        val guiScreen = Minecraft.getInstance().screen
        val openGui = guiScreen?.javaClass?.name ?: "none"
        val isInNeuPv = openGui == "io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer"
        if (isInNeuPv) return
        guiScreen?.let {
            if (it !is InventoryScreen && it !is ContainerScreen && it !is SignEditScreen) return
            if (it is SignEditScreen && !it.isGardenSign()) return
        }

        if (lastHotkeyPressed.passedSince() < 500.milliseconds) return
        lastHotkeyPressed = SimpleTimeMark.now()

        openGuiPositionEditor(hotkeyReminder = false)
    }

    @HandleEvent
    fun onTick() {
        lastMovedGui?.let {
            GuiPositionMovedEvent(it).post()
            lastMovedGui = null
        }
    }

    @JvmStatic
    fun add(position: Position, posLabel: String, width: Int, height: Int) {
        val name = position.getOrSetInternalName {
            if (posLabel == "none") "none ${StringUtils.generateRandomId()}" else posLabel
        }
        currentPositions[name] = position
        currentBorderSize[posLabel] = Pair(width, height)
    }

    private var lastHotkeyReminded = SimpleTimeMark.farPast()

    @JvmStatic
    fun openGuiPositionEditor(hotkeyReminder: Boolean) {
        SkyHanniMod.shouldCloseScreen = false
        SkyHanniMod.screenToOpen = GuiPositionEditor(
            currentPositions.values.toList(),
            2,
            Minecraft.getInstance().screen as? SkyHanniGuiContainer,
        )
        if (hotkeyReminder && lastHotkeyReminded.passedSince() > 30.minutes) {
            lastHotkeyReminded = SimpleTimeMark.now()
            ChatUtils.chat(
                "§eTo edit hidden GUI elements:\n" +
                    " §7- §e1. Set a key in /sh edit.\n" +
                    " §7- §e2. Click that key while the GUI element is visible.",
            )
        }
    }

    @JvmStatic
    fun renderLast(context: GuiGraphics) {
        if (GlobalRender.renderDisabled) return
        if (!isInGui()) return

        DrawContextUtils.setContext(context)
        DrawContextUtils.translate(0f, 0f, 200f)

        RenderData.renderOverlay(context)

        DrawContextUtils.pushPop {
            GuiRenderEvent.ChestGuiOverlayRenderEvent(context).post()
        }

        DrawContextUtils.translate(0f, 0f, -200f)
        DrawContextUtils.clearContext()
    }

    fun isInGui() = Minecraft.getInstance().screen is GuiPositionEditor

    fun Position.getDummySize(random: Boolean = false): Vector2i {
        if (random) return Vector2i(5, 5)
        val (x, y) = currentBorderSize[internalName] ?: return Vector2i(1, 1)
        return Vector2i((x * effectiveScale).toInt(), (y * effectiveScale).toInt())
    }

    fun Position.getAbsX() = getAbsX0(getDummySize(true).x)

    fun Position.getAbsY() = getAbsY0(getDummySize(true).y)

    fun handleGuiPositionMoved(guiName: String) {
        lastMovedGui = guiName
    }
}

// TODO remove
class Vector2i(val x: Int, val y: Int)
