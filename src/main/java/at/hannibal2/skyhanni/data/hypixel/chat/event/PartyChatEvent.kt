package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import net.minecraft.util.IChatComponent

class PartyChatEvent(
    author: String,
    message: String,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, chatComponent, blockedReason) {
    val cleanedAuthor by lazy {
        author.cleanPlayerName()
    }
}
