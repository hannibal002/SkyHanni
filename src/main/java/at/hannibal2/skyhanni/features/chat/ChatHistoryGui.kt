package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import io.github.notenoughupdates.moulconfig.internal.GlScissorStack
import io.github.notenoughupdates.moulconfig.internal.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiUtilRenderComponents
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
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

    override fun onDrawScreen(context: DrawContext, originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        drawDefaultBackground(context, originalMouseX, originalMouseY, partialTicks)
        var queuedTooltip: List<String>? = null
        context.matrices.pushMatrix()
        val l = (width / 2.0 - w / 2.0).toInt()
        val t = (height / 2.0 - h / 2.0).toInt()
        context.matrices.translate(l + 0.0, t + 0.0, 0.0)
        RenderUtils.drawFloatingRectDark(0, 0, w, h)
        context.matrices.translate(5.0, 5.0 - scroll, 0.0)
        val mouseX = originalMouseX - l
        val isMouseButtonDown = mouseX in 0..w && originalMouseY in t..(t + h) && MouseCompat.isButtonDown(0)
        var mouseY = originalMouseY - (t - scroll).toInt()
        val sr = ScaledResolution(mc)
        GlScissorStack.push(l + 5, t + 5, w + l - 5, h + t - 5, sr)

        for (msg in history) {
            drawString(fontRenderer(), msg.actionKind.renderedString, 0, 0, -1)
            drawString(fontRenderer(), msg.actionReason, ChatManager.ActionKind.maxLength + 5, 0, -1)
            var size = drawMultiLineText(context, msg.message, ChatManager.ActionKind.maxLength + reasonMaxLength + 10)
            msg.modified?.let {
                drawString(
                    fontRenderer(),
                    "§e§lNEW TEXT",
                    0, size * 10, -1,
                )
                size += drawMultiLineText(context, it, ChatManager.ActionKind.maxLength + reasonMaxLength + 10)
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
        GlScissorStack.pop(sr)
        wasMouseButtonDown = isMouseButtonDown
        context.matrices.popMatrix()
        queuedTooltip?.let { tooltip ->
            RenderableTooltips.setTooltipForRender(tooltip.map { Renderable.string(it) })
        }
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    private fun splitLine(comp: IChatComponent): List<IChatComponent> {
        return GuiUtilRenderComponents.splitText(
            comp,
            w - (ChatManager.ActionKind.maxLength + reasonMaxLength + 10 + 10),
            fontRenderer(),
            false,
            true,
        )
    }

    override fun onInitGui() {
        if (this.scroll < 0) {
            setScroll(1000000000.0)
        }
    }

    private fun setScroll(newScroll: Double) {
        this.scroll = newScroll.coerceAtMost(historySize - h + 10.0).coerceAtLeast(0.0)
    }

    private fun drawMultiLineText(context: DrawContext, comp: IChatComponent, xPos: Int): Int {
        val modifiedSplitText = splitLine(comp)
        for (line in modifiedSplitText) {
            drawString(
                fontRenderer(),
                line.formattedText,
                xPos,
                0,
                -1,
            )
            context.matrices.translate(0F, 10F, 0F)
        }
        return modifiedSplitText.size
    }

    private fun fontRenderer() = Minecraft.getMinecraft().fontRendererObj

    override fun onHandleMouseInput() {
        setScroll(scroll - MouseCompat.getScrollDelta())
    }
}
