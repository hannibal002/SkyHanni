package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import net.minecraft.network.chat.Component

object PartyChatEvent {

    class Allow(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason) {
        val cleanedAuthor = author.cleanPlayerName()
    }

    class Modify(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason) {
        val cleanedAuthor = author.cleanPlayerName()
    }
}
