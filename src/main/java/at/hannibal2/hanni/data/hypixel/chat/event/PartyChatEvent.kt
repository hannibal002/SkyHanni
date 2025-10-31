package at.hannibal2.hanni.data.hypixel.chat.event

import at.hannibal2.hanni.utils.ComponentSpan
import at.hannibal2.hanni.utils.StringUtils.cleanPlayerName
import net.minecraft.util.IChatComponent

class PartyChatEvent(
    authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(authorComponent, messageComponent, chatComponent, blockedReason) {
    val cleanedAuthor = author.cleanPlayerName()
}
