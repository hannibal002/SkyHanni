package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.network.chat.Component

object GuildChatEvent {

    class Allow(
        author: Component,
        message: Component,
        val guildRank: Component?,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    class Modify(
        author: Component,
        message: Component,
        val guildRank: Component?,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent, blockedReason)
}
