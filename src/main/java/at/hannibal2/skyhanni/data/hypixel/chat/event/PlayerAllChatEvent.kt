package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.toChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor

object PlayerAllChatEvent {

    class Allow(
        val levelComponent: Component?,
        val privateIslandRank: Component?,
        val privateIslandGuest: Component?,
        val chatColor: TextColor,
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason) {
        val levelColor = TextHelper.sampleStyleAtStart(levelComponent)?.color?.toChatFormatting()
        val level = levelComponent?.string?.toInt()
        val isAGuest get() = privateIslandGuest != null
    }

    class Modify(
        val levelComponent: Component?,
        val privateIslandRank: Component?,
        val privateIslandGuest: Component?,
        val chatColor: TextColor,
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason) {
        val levelColor = TextHelper.sampleStyleAtStart(levelComponent)?.color?.toChatFormatting()
        val level = levelComponent?.string?.toInt()
        val isAGuest get() = privateIslandGuest != null
    }
}
