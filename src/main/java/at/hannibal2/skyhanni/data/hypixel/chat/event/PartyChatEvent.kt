package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

object PartyChatEvent {

    class Allow(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason) {
        val authorName = author.cleanPlayerName()

        override val cleanMessage: String = messageComponent.getText().removeColor()
    }

    class Modify(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason) {
        val authorName = author.cleanPlayerName()

        override val cleanMessage: String
            get() = messageComponent.getText().removeColor()
    }
}
