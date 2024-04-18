package at.hannibal2.skyhanni.data.hypixel.chat.event

class PrivateMessageChatEvent(
    val direction: String?,
    author: String,
    message: String,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, blockedReason)
