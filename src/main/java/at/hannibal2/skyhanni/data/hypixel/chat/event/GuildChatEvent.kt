package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.data.hypixel.chat.AbstractChatEvent

class GuildChatEvent(
    author: String,
    message: String,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, blockedReason)
