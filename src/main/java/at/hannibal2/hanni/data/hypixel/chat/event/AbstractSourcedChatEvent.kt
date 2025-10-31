package at.hannibal2.hanni.data.hypixel.chat.event

import at.hannibal2.hanni.events.chat.AbstractChatEvent
import at.hannibal2.hanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent

abstract class AbstractSourcedChatEvent(
    val authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(messageComponent, chatComponent, blockedReason) {
    val author = authorComponent.getText()
}
