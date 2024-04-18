package at.hannibal2.skyhanni.data.hypixel.chat.event

class PlayerShowItemChatEvent(
    val levelColor: String?,
    val level: Int?,
    author: String,
    message: String,
    val action: String,
    val itemName: String,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, blockedReason)
