package at.hannibal2.skyhanni.data.hypixel.chat

class PlayerAllChatEvent(
    val levelColor: String?,
    val level: Int?,
    author: String,
    message: String,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, blockedReason)
