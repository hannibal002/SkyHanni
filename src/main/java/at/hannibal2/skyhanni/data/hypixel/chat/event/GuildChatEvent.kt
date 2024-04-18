package at.hannibal2.skyhanni.data.hypixel.chat.event

class GuildChatEvent(
    author: String,
    message: String,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, blockedReason)
