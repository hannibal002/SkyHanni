package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import net.minecraft.client.Minecraft
import net.minecraft.util.IChatComponent

class ChatHistoryGui(private val history: List<ChatManager.MessageFilteringResult>) : SkyhanniBaseScreen() {

    private var scroll = -1.0
    private val w = 500
    private var wasMouseButtonDown = false
    private val h = 300
    private val reasonMaxLength =
        history.maxOf { reasonLength(it) }

    private fun reasonLength(result: ChatManager.MessageFilteringResult): Int =
        result.actionReason?.let { fontRenderer().getStringWidth(it) } ?: 0

    private val historySize =
        history.sumOf { splitLine(it.message).size * 10 + (it.modified?.let { mod -> splitLine(mod).size * 10 } ?: 0) }

    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        drawDefaultBackground(originalMouseX, originalMouseY, partialTicks)
        var queuedTooltip: List<String>? = null
        DrawContextUtils.pushMatrix()
        val l = (width / 2.0 - w / 2.0).toInt()
        val t = (height / 2.0 - h / 2.0).toInt()
        DrawContextUtils.translate(l + 0.0, t + 0.0, 0.0)
        GuiRenderUtils.drawFloatingRectDark(0, 0, w, h)
        DrawContextUtils.translate(5.0, 5.0 - scroll, 0.0)
        val mouseX = originalMouseX - l
        val isMouseButtonDown = mouseX in 0..w && originalMouseY in t..(t + h) && MouseCompat.isButtonDown(0)
        var mouseY = originalMouseY - (t - scroll).toInt()
        GuiRenderUtils.enableScissor(l + 5, t + 5, w + l - 5, h + t - 5)

        for (msg in history) {
            GuiRenderUtils.drawString(msg.actionKind.renderedString, 0, 0, -1)
            msg.actionReason?.let { GuiRenderUtils.drawString(it, ChatManager.ActionKind.maxLength + 5, 0, -1) }
            var size = drawMultiLineText(msg.message, ChatManager.ActionKind.maxLength + reasonMaxLength + 10)
            msg.modified?.let {
                GuiRenderUtils.drawString("§e§lNEW TEXT", 0, 0, -1)
                size += drawMultiLineText(it, ChatManager.ActionKind.maxLength + reasonMaxLength + 10)
            }
            val isHovered = mouseX in 0..w && mouseY in 0..(size * 10)
            if (isHovered && msg.hoverInfo.isNotEmpty())
                queuedTooltip = msg.hoverInfo
            if (isHovered && KeyboardManager.isShiftKeyDown() && msg.hoverExtraInfo.isNotEmpty())
                queuedTooltip = msg.hoverExtraInfo
            if (isHovered && (isMouseButtonDown && !wasMouseButtonDown)) {
                if (KeyboardManager.isShiftKeyDown()) {
                    OSUtils.copyToClipboard(IChatComponent.Serializer.componentToJson(msg.message))
                    ChatUtils.chat("Copied structured chat line to clipboard", false)
                } else {
                    val message = msg.message.formattedText.stripHypixelMessage()
                    OSUtils.copyToClipboard(message)
                    ChatUtils.chat("Copied chat line to clipboard")
                }
            }
            mouseY -= size * 10
        }
        GuiRenderUtils.disableScissor()
        wasMouseButtonDown = isMouseButtonDown
        DrawContextUtils.popMatrix()
        queuedTooltip?.let { tooltip ->
            RenderableTooltips.setTooltipForRender(tooltip.map { Renderable.string(it) })
        }
    }

    private fun splitLine(comp: IChatComponent): List<String> {
        return comp.formattedText.splitLines(w - (ChatManager.ActionKind.maxLength + reasonMaxLength + 10 + 10)).split("\n")
    }

    override fun onInitGui() {
        if (this.scroll < 0) {
            setScroll(1000000000.0)
        }
    }

    private fun setScroll(newScroll: Double) {
        this.scroll = newScroll.coerceAtMost(historySize - h + 10.0).coerceAtLeast(0.0)
    }

    private fun drawMultiLineText(comp: IChatComponent, xPos: Int): Int {
        val lines = splitLine(comp)
        for (line in lines) {
            GuiRenderUtils.drawString(line, xPos, 0, -1)
            DrawContextUtils.translate(0f, 10f, 0f)
        }
        return lines.size
    }

    private fun fontRenderer() = Minecraft.getMinecraft().fontRendererObj

    override fun onHandleMouseInput() {
        setScroll(scroll - MouseCompat.getScrollDelta())
    }
}
