package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.SkyHanniScreenTheme
import at.hannibal2.skyhanni.utils.compat.convertToJsonString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ComponentRenderUtils
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

object ChatHistoryGui {

    private const val LIST_WIDTH = 500
    private const val LIST_HEIGHT = 300
    private val copyCoroutine = CoroutineSettings("chat history copy to clipboard")

    private fun ChatManager.MessageFilteringResult.getReason(): String? =
        actionReason ?: modifiedReason

    private fun reasonLength(result: ChatManager.MessageFilteringResult): Int =
        result.getReason()?.let { Minecraft.getInstance().font.width(it) } ?: 0

    fun buildContent(screen: ChatHistoryScreen): Renderable {
        val history = screen.history
        val reasonMaxLength = history.maxOfOrNull { reasonLength(it) } ?: 0
        val xOffset = ChatManager.ActionKind.maxLength + reasonMaxLength + 10
        val wrapWidth = LIST_WIDTH - xOffset - 10

        val rows = history.map { msg -> buildRow(msg, wrapWidth, xOffset) }

        return Renderable.scrollList(
            rows,
            height = LIST_HEIGHT,
            scrollValue = screen.scrollValue,
            bypassChecks = true,
            showScrollbar = true,
            scrollbarTrackColor = SkyHanniScreenTheme.COLOR_SCROLLBAR_TRACK,
            scrollbarThumbColor = SkyHanniScreenTheme.COLOR_SCROLLBAR_THUMB,
        )
    }

    /**
     * Builds a single row Renderable for a message filtering result.
     *
     * @param msg The message filtering result to render.
     * @param wrapWidth The available pixel width for wrapping message text.
     * @param xOffset The horizontal offset at which message text begins.
     */
    private fun buildRow(
        msg: ChatManager.MessageFilteringResult,
        wrapWidth: Int,
        xOffset: Int,
    ): Renderable {
        val messageLines = wrapComponent(msg.message, wrapWidth)
        val modifiedLines = msg.modified?.let { wrapComponent(it, wrapWidth) }.orEmpty()

        val rowContent = buildRowContent(msg, messageLines, modifiedLines, xOffset, wrapWidth)

        val withHover = Renderable.hoverable(
            Renderable.drawInsideRoundedRect(rowContent, SkyHanniScreenTheme.COLOR_ROW_HOVER, padding = 2, radius = 3),
            rowContent,
            bypassChecks = true,
        )

        val tooltip = msg.hoverInfo.takeIf { it.isNotEmpty() }?.map(StringRenderable::from)
        val extraTooltip = msg.hoverExtraInfo.takeIf { it.isNotEmpty() }?.map(StringRenderable::from)

        val withTooltip = if (tooltip != null || extraTooltip != null) {
            object : Renderable by withHover {
                override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                    withHover.render(mouseOffsetX, mouseOffsetY)
                    if (isHovered(mouseOffsetX, mouseOffsetY)) {
                        val tip = if (KeyboardManager.isShiftKeyDown()) extraTooltip ?: tooltip else tooltip
                        tip?.let { RenderableTooltips.setTooltipForRender(it) }
                    }
                }
            }
        } else withHover

        return Renderable.clickable(
            withTooltip,
            onLeftClick = {
                copyCoroutine.launch {
                    val (target, copyType) = if (KeyboardManager.isShiftKeyDown()) {
                        msg.message.convertToJsonString() to "structured"
                    } else {
                        msg.message.formattedTextCompat().stripHypixelMessage() to ""
                    }
                    val copied = ClipboardUtils.copyToClipboardAsync(target).await() ?: false
                    if (!copied) ChatUtils.chat("Failed to copy $copyType to clipboard")
                    else ChatUtils.chat("Copied $copyType chat line to clipboard")
                }
            },
            bypassChecks = true,
        )
    }

    /**
     * Builds the content Renderable for a single row without hover/click wrappers.
     *
     * @param msg The message filtering result to render.
     * @param messageLines The wrapped lines of the main message.
     * @param modifiedLines The wrapped lines of the modified message, if any.
     * @param xOffset The combined pixel width of the action and reason label columns.
     * @param wrapWidth The pixel width available for rendering message text.
     */
    private fun buildRowContent(
        msg: ChatManager.MessageFilteringResult,
        messageLines: List<FormattedCharSequence>,
        modifiedLines: List<FormattedCharSequence>,
        xOffset: Int,
        wrapWidth: Int,
    ): Renderable {
        val actionLabel = Renderable.fixedSizeLine(
            Renderable.text(msg.actionKind.renderedString),
            width = ChatManager.ActionKind.maxLength,
        )
        val reasonLabel = Renderable.fixedSizeLine(
            msg.getReason()?.let { Renderable.text(it) } ?: Renderable.empty(),
            width = xOffset - ChatManager.ActionKind.maxLength,
        )
        val msgBlock = if (msg.modified == null) {
            formattedLines(messageLines, wrapWidth)
        } else {
            Renderable.vertical(
                listOf(
                    formattedLines(messageLines, wrapWidth),
                    Renderable.text("§e§lNEW TEXT"),
                    formattedLines(modifiedLines, wrapWidth),
                ),
                spacing = 1,
            )
        }
        return Renderable.horizontal(
            listOf(actionLabel, reasonLabel, msgBlock),
            spacing = 0,
        )
    }

    private fun formattedLines(lines: List<FormattedCharSequence>, wrapWidth: Int): Renderable =
        Renderable.vertical(lines.map { line -> formattedCharRenderable(line, wrapWidth) }, spacing = 1)

    private fun formattedCharRenderable(line: FormattedCharSequence, wrapWidth: Int): Renderable = object : Renderable {
        override val width = wrapWidth
        override val height = 10
        override val horizontalAlign = HorizontalAlignment.LEFT
        override val verticalAlign = VerticalAlignment.TOP

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            GuiRenderUtils.drawString(line, 0, 0, -1)
        }
    }

    private fun wrapComponent(comp: Component, width: Int): List<FormattedCharSequence> =
        ComponentRenderUtils.wrapComponents(comp, width, Minecraft.getInstance().font)
}
