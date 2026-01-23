package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object GuildChatEvent {

    class Allow(
        author: ComponentSpan,
        message: ComponentSpan,
        val guildRank: ComponentSpan?,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    class Modify(
        author: ComponentSpan,
        message: ComponentSpan,
        val guildRank: ComponentSpan?,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent, blockedReason)
}
