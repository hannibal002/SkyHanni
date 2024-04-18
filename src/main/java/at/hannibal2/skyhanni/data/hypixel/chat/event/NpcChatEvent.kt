package at.hannibal2.skyhanni.data.hypixel.chat.event

class NpcChatEvent(
    author: String,
    message: String,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, blockedReason)
