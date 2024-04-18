package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.events.LorenzEvent

open class AbstractChatEvent(
    val author: String,
    val message: String,
    var blockedReason: String? = null,
) : LorenzEvent()
