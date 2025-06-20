package at.hannibal2.skyhanni.utils.compat

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
        DrawContextUtils.setContext(DrawContext())
        onDrawScreen(mouseX, mouseY, partialTicks)
        DrawContextUtils.clearContext()
    }
    //#else
    //$$ override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    //$$    super.render(context, mouseX, mouseY, delta)
    //$$    DrawContextUtils.setContext(context)
    //$$    onDrawScreen(mouseX, mouseY, delta)
    //$$    DrawContextUtils.clearContext()
    //$$ }
    //$$
    //$$ override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
    //$$         this.renderDarkening(context)
    //$$     }
    //#endif

    open fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {}

    //#if MC < 1.21
    final override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        onMouseClicked(mouseX, mouseY, mouseButton)
    }
    //#else
    //$$ override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
    //$$     onMouseClicked(mouseX.toInt(), mouseY.toInt(), mouseButton)
    //$$     return super.mouseClicked(mouseX, mouseY, mouseButton)
    //$$ }
    //#endif

    open fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {}

    //#if MC < 1.21
    final override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        onKeyTyped(typedChar, keyCode)
    }
    //#else
    //$$ override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    //$$     // TODO confirm if keyCode.toChar() is correct
    //$$     onKeyTyped(keyCode.toChar(), keyCode)
    //$$     return super.keyPressed(keyCode, scanCode, modifiers)
    //$$ }
    //#endif

    open fun onKeyTyped(typedChar: Char, keyCode: Int) {}

    //#if MC < 1.21
    final override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        onMouseReleased(mouseX, mouseY, state)
    }
    //#else
    //$$ override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
    //$$     onMouseReleased(mouseX.toInt(), mouseY.toInt(), button)
    //$$     return super.mouseReleased(mouseX, mouseY, button)
    //$$ }
    //#endif

    open fun onMouseReleased(originalMouseX: Int, originalMouseY: Int, state: Int) {}

    //#if MC < 1.21
    final override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        onMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }
    //#else
    //$$ override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
    //$$     // TODO there is no timeSince last click in modern
    //$$     onMouseClickMove(mouseX.toInt(), mouseY.toInt(), button, 0L)
    //$$     return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    //$$ }
    //#endif

    open fun onMouseClickMove(originalMouseX: Int, originalMouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {}

    //#if MC < 1.21
    final override fun handleMouseInput() {
        super.handleMouseInput()
        onHandleMouseInput()
    }
    //#else
    //$$ override fun mouseMoved(mouseX: Double, mouseY: Double) {
    //$$     onHandleMouseInput()
    //$$     super.mouseMoved(mouseX, mouseY)
    //$$ }
    //$$
    //$$ override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
    //$$     onHandleMouseInput()
    //$$     return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    //$$ }
    //#endif

    open fun onHandleMouseInput() {}

    //#if MC < 1.21
    final override fun onGuiClosed() {
        super.onGuiClosed()
        guiClosed()
    }
    //#else
    //$$ override fun close() {
    //$$     super.close()
    //$$     guiClosed()
    //$$ }
    //#endif

    open fun guiClosed() {}

    //#if MC < 1.21
    final override fun initGui() {
        super.initGui()
        onInitGui()
    }
    //#else
    //$$ override fun init() {
    //$$     super.init()
    //$$     onInitGui()
    //$$ }
    //#endif

    open fun onInitGui() {}

    fun drawDefaultBackground(mouseX: Int, mouseY: Int, partialTicks: Float) {
        //#if MC < 1.21
        drawDefaultBackground()
        //#else
        //$$ renderDarkening(DrawContextUtils.drawContext)
        //#endif
    }
}
