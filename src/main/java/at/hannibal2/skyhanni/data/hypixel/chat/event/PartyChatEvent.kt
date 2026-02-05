package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import net.minecraft.network.chat.Component

object PartyChatEvent {

    class Allow(
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason) {
        val cleanedAuthor = author.cleanPlayerName()
    }

    class Modify(
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason) {
        val cleanedAuthor = author.cleanPlayerName()
    }
}
