package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.InputCode
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import net.minecraft.client.gui.screens.ChatScreen
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InputSequenceTracker {

    @Suppress("SpellCheckingInspection")
    private val SEQUENCE = "notrpwia".reversed()

    // What is left in the chat input after the "t" of the sequence opened it
    private const val CHAT_LEFTOVER = "on"

    private const val PIXEL = 4
    private const val SHAPE_WIDTH = 56
    private const val SHAPE_HEIGHT = 25

    private const val BODY_COLOR = 0xFF4A5D3A.toInt()
    private const val BASE_COLOR = 0xFF2B2B2B.toInt()
    private const val DETAIL_COLOR = 0xFF555555.toInt()

    private var buffer = ""
    private var lastInput = SimpleTimeMark.farPast()
    private var lettersInChat = 0

    @HandleEvent
    private fun onKeyDown(event: KeyDownEvent) {
        // Anything that is not a single character breaks the sequence, otherwise editing keys
        // like backspace would change the chat input while leaving the buffer untouched.
        // The counter goes with it, since it can no longer describe what is in that input
        val typed = event.key.displayName.lowercase()
        if (typed.length != 1) {
            buffer = ""
            lettersInChat = 0
            return
        }

        if (lastInput.passedSince() > 2.seconds) buffer = ""
        lastInput = SimpleTimeMark.now()

        // Counts single character keys pressed while a chat is open. The input field alone
        // cannot tell us who opened that chat, since the player can edit it with the mouse
        // without ever reaching this handler
        lettersInChat = if (MinecraftCompat.screen is ChatScreen) lettersInChat + 1 else 0

        // takeLast caps the buffer at the sequence length, so it can never grow
        buffer = (buffer + typed).takeLast(SEQUENCE.length)
        if (buffer != SEQUENCE) return
        buffer = ""

        // Delayed by a tick for two reasons: vanilla still forwards this key press after our
        // mixin returns and would close a screen opened here, and the last letter only reaches
        // the chat input via charTyped afterward
        DelayedRun.runNextTick(::handleSequence)
    }

    private fun handleSequence() {
        closeUntouchedChat()

        if (!TimeUtils.isAprilFoolsDay) {
            ChatUtils.chat("Error: could not spawn a tank: wrong game", prefix = false)
            return
        }
        ChatUtils.chat("April Fools! Here is your tank... almost.", prefix = false)
        MinecraftCompat.screen = SequenceOverlay()
    }

    /**
     * Only closes a chat that the sequence itself opened, recognized by the letter count
     * plus the input matching [CHAT_LEFTOVER] exactly. This assumes the default chat key,
     * since only then does the "t" of the sequence open a chat at all.
     *
     * Blanking the input before closing is required, since [ChatScreen.removed] stores
     * anything non-blank as a draft.
     */
    private fun closeUntouchedChat() {
        if (lettersInChat != CHAT_LEFTOVER.length) return
        val screen = MinecraftCompat.screen as? ChatScreen ?: return
        if (screen.input.value != CHAT_LEFTOVER) return
        screen.input.value = ""
        screen.onClose()
    }

    private class SequenceOverlay : SkyHanniBaseScreen() {

        private val openedAt = SimpleTimeMark.now()

        override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            drawDefaultBackground(mouseX, mouseY, partialTicks)

            box(0, 18, 44, 25, BASE_COLOR)
            box(0, 8, 44, 18, BODY_COLOR)
            box(12, 0, 30, 8, BODY_COLOR)
            box(30, 2, 56, 6, BODY_COLOR)
            repeat(7) {
                val left = 2 + it * 6
                box(left, 19, left + 4, 24, DETAIL_COLOR)
            }
        }

        // Coordinates are in shape units, each one is PIXEL screen pixels wide,
        // and the shape as a whole is centered on the screen
        private fun box(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
            val originX = (width - SHAPE_WIDTH * PIXEL) / 2
            val originY = (height - SHAPE_HEIGHT * PIXEL) / 2
            GuiRenderUtils.drawRect(
                originX + left * PIXEL,
                originY + top * PIXEL,
                originX + right * PIXEL,
                originY + bottom * PIXEL,
                color,
            )
        }

        override fun tick() {
            super.tick()
            if (openedAt.passedSince() > 7.seconds) onClose()
        }

        override fun onKeyTyped(typedChar: Char?, key: InputCode?) {
            onClose()
        }

        override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: InputCode) {
            onClose()
        }
    }
}
