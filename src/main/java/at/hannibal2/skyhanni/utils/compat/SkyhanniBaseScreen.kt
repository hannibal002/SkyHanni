package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
//#if MC > 1.21
//$$ import net.minecraft.client.gui.DrawContext
//#endif

@Suppress("UnusedParameter")
abstract class SkyhanniBaseScreen : GuiScreen(
    //#if MC > 1.20
    //$$ net.minecraft.text.Text.empty()
    //#elseif MC > 1.16
    //$$ net.minecraft.network.chat.TextComponent.EMPTY
    //#endif
) {

    val mc: Minecraft = Minecraft.getMinecraft()

    //#if MC < 1.21
    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        postDrawScreen(DrawContext(), mouseX, mouseY, partialTicks)
    }
    //#else
    //$$ override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    //$$    super.render(context, mouseX, mouseY, delta)
    //$$    postDrawScreen(context, mouseX, mouseY, delta)
    //$$ }
    //$$
    //$$ override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
    //$$     this.renderDarkening(context)
    //$$ }
    //#endif

    private fun postDrawScreen(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        DrawContextUtils.setContext(context)
        try {
            onDrawScreen(mouseX, mouseY, partialTicks)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while drawing screen", "screen" to this)
        }
        DrawContextUtils.clearContext()
    }
    open fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    //#if MC < 1.21
    final override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        postMouseClicked(mouseX, mouseY, mouseButton)
    }
    //#else
    //$$ override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
    //$$     postMouseClicked(mouseX.toInt(), mouseY.toInt(), mouseButton)
    //$$     postHandleMouseInput()
    //$$     return super.mouseClicked(mouseX, mouseY, mouseButton)
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

    //#if MC < 1.21
    final override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        postKeyTyped(typedChar, keyCode)
    }
    //#else
    //$$ override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    //$$     postKeyTyped(null, keyCode)
    //$$     return super.keyPressed(keyCode, scanCode, modifiers)
    //$$ }
    //$$
    //$$ override fun charTyped(chr: Char, modifiers: Int): Boolean {
    //$$     postKeyTyped(chr, null)
    //$$     return super.charTyped(chr, modifiers)
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

    //#if MC < 1.21
    final override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        postMouseReleased(mouseX, mouseY, state)
    }
    //#else
    //$$ override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
    //$$     postMouseReleased(mouseX.toInt(), mouseY.toInt(), button)
    //$$     postHandleMouseInput()
    //$$     return super.mouseReleased(mouseX, mouseY, button)
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

    //#if MC < 1.21
    final override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        postMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }
    //#else
    //$$ override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
    //$$     // TODO there is no timeSince last click in modern
    //$$     postMouseClickMove(mouseX.toInt(), mouseY.toInt(), button, 0L)
    //$$     postHandleMouseInput()
    //$$     return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
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

    //#if MC < 1.21
    final override fun handleMouseInput() {
        super.handleMouseInput()
        postHandleMouseInput()
    }
    //#else
    //$$ override fun mouseMoved(mouseX: Double, mouseY: Double) {
    //$$     postHandleMouseInput()
    //$$     super.mouseMoved(mouseX, mouseY)
    //$$ }
    //$$
    //$$ override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
    //$$     postHandleMouseInput()
    //$$     return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    //$$ }
    //#endif

    private fun postHandleMouseInput() {
        try {
            onHandleMouseInput()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while handling mouse input", "screen" to this)
        }
    }
    open fun onHandleMouseInput() {}

    //#if MC < 1.21
    final override fun onGuiClosed() {
        super.onGuiClosed()
        postGuiClosed()
    }
    //#else
    //$$ override fun close() {
    //$$     super.close()
    //$$     postGuiClosed()
    //$$ }
    //#endif

    private fun postGuiClosed() {
        try {
            guiClosed()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while closing GUI", "screen" to this)
        }
    }
    open fun guiClosed() {}

    //#if MC < 1.21
    final override fun initGui() {
        super.initGui()
        postInitGui()
    }
    //#else
    //$$ override fun init() {
    //$$     super.init()
    //$$     postInitGui()
    //$$ }
    //#endif

    private fun postInitGui() {
        try {
            onInitGui()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while initializing GUI", "screen" to this)
        }
    }
    open fun onInitGui() {}

    fun drawDefaultBackground(mouseX: Int, mouseY: Int, partialTicks: Float) {
        //#if MC < 1.21
        drawDefaultBackground()
        //#else
        //$$ renderDarkening(DrawContextUtils.drawContext)
        //#endif
    }
}
