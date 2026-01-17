package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
//#if MC > 1.21.8
//$$ import net.minecraft.client.input.MouseButtonEvent
//$$ import net.minecraft.client.input.CharacterEvent
//$$ import net.minecraft.client.input.KeyEvent
//#endif

@Suppress("UnusedParameter", "TooManyFunctions")
abstract class SkyHanniBaseScreen : Screen(Component.empty()) {

    val mc: Minecraft = Minecraft.getInstance()

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        postDrawScreen(context, mouseX, mouseY, delta)
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        try {
            this.renderMenuBackground(context)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while rendering background", "screen" to this)
        }
    }

    private fun postDrawScreen(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        DrawContextUtils.setContext(context)
        try {
            onDrawScreen(mouseX, mouseY, partialTicks)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while drawing screen", "screen" to this)
        }
        DrawContextUtils.clearContext()
    }

    open fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    //#if MC < 1.21.9
    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
        postMouseClicked(mouseX.toInt(), mouseY.toInt(), mouseButton)
        postHandleMouseInput()
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }
    //#else
    //$$ override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
    //$$     postMouseClicked(click.x.toInt(), click.y.toInt(), click.button())
    //$$     postHandleMouseInput()
    //$$     return super.mouseClicked(click, doubled)
    //$$ }
    //#endif

    private fun postMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        try {
            onMouseClicked(originalMouseX, originalMouseY, mouseButton)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while clicking mouse", "screen" to this)
        }
    }

    open fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {}

    //#if MC < 1.21.9
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        postKeyTyped(null, keyCode)
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        postKeyTyped(chr, null)
        return super.charTyped(chr, modifiers)
    }
    //#else
    //$$ override fun keyPressed(input: KeyEvent): Boolean {
    //$$     postKeyTyped(null, input.key)
    //$$     return super.keyPressed(input)
    //$$ }
    //$$
    //$$ override fun charTyped(input: CharacterEvent): Boolean {
    //$$     postKeyTyped(input.codepoint.toChar(), null)
    //$$     return super.charTyped(input)
    //$$ }
    //#endif

    private fun postKeyTyped(typedChar: Char?, keyCode: Int?) {
        try {
            onKeyTyped(typedChar, keyCode)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while typing key", "screen" to this)
        }
    }

    open fun onKeyTyped(typedChar: Char?, keyCode: Int?) {}

    //#if MC < 1.21.9
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        postMouseReleased(mouseX.toInt(), mouseY.toInt(), button)
        postHandleMouseInput()
        return super.mouseReleased(mouseX, mouseY, button)
    }
    //#else
    //$$ override fun mouseReleased(click: MouseButtonEvent): Boolean {
    //$$     postMouseReleased(click.x.toInt(), click.y.toInt(), click.button())
    //$$     postHandleMouseInput()
    //$$     return super.mouseReleased(click)
    //$$ }
    //#endif

    private fun postMouseReleased(originalMouseX: Int, originalMouseY: Int, state: Int) {
        try {
            onMouseReleased(originalMouseX, originalMouseY, state)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while releasing mouse", "screen" to this)
        }
    }

    open fun onMouseReleased(originalMouseX: Int, originalMouseY: Int, state: Int) {}

    //#if MC < 1.21.9
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        // TODO there is no timeSince last click in modern
        postMouseClickMove(mouseX.toInt(), mouseY.toInt(), button, 0L)
        postHandleMouseInput()
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    //#else
    //$$ override fun mouseDragged(click: MouseButtonEvent, mouseX: Double, mouseY: Double): Boolean {
    //$$     // TODO idk if mouseX is correct or if it should be click.x
    //$$     postMouseClickMove(mouseX.toInt(), mouseY.toInt(), click.button(), 0L)
    //$$     postHandleMouseInput()
    //$$     return super.mouseDragged(click, mouseX, mouseY)
    //$$ }
    //#endif

    private fun postMouseClickMove(originalMouseX: Int, originalMouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        try {
            onMouseClickMove(originalMouseX, originalMouseY, clickedMouseButton, timeSinceLastClick)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while clicking and moving mouse", "screen" to this)
        }
    }

    open fun onMouseClickMove(originalMouseX: Int, originalMouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {}

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        postHandleMouseInput()
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        postHandleMouseInput()
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun postHandleMouseInput() {
        try {
            onHandleMouseInput()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while handling mouse input", "screen" to this)
        }
    }

    open fun onHandleMouseInput() {}

    override fun onClose() {
        super.onClose()
        postGuiClosed()
    }

    private fun postGuiClosed() {
        try {
            guiClosed()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while closing GUI", "screen" to this)
        }
    }

    open fun guiClosed() {}

    override fun init() {
        super.init()
        postInitGui()
    }

    private fun postInitGui() {
        try {
            onInitGui()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while initializing GUI", "screen" to this)
        }
    }

    open fun onInitGui() {}

    fun drawDefaultBackground(mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderMenuBackground(DrawContextUtils.drawContext)
    }
}
