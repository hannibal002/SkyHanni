package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.center
import at.hannibal2.skyhanni.utils.chat.TextHelper.width
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

/**
 * Sends a line that belongs under Hypixel's end-of-fight summary and should sit in the middle just
 * like the rest of it.
 *
 * Hypixel centres each of those lines on its own, so the padding is measured rather than fixed: a
 * longer line simply gets fewer spaces. A fixed indent only ever fits one message length, which is
 * why the weight line and the DPS line cannot share one.
 */
internal object EndFightSummary {

    /** The prefix [ChatUtils] puts in front. It is private there, so its width is taken from a copy. */
    private const val CHAT_PREFIX = "[SkyHanni] "

    fun send(message: String, withPrefix: Boolean) {
        // Whatever the prefix occupies is room the line no longer has for centring itself.
        val room = MinecraftCompat.hud.chat.width - if (withPrefix) CHAT_PREFIX.width() else 0
        ChatUtils.chat(message.asComponent().center(room), prefix = withPrefix)
    }
}
