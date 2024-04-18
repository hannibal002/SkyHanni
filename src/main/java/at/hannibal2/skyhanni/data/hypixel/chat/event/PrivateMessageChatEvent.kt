package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.util.IChatComponent

class PrivateMessageChatEvent(
    val direction: String?,
    author: String,
    message: String,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, chatComponent, blockedReason)
