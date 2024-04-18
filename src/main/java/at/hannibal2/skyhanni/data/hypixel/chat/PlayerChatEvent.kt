package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.events.LorenzEvent

class PlayerChatEvent(
    val levelColor: String?,
    val level: Int?,
    val author: String,
    val message: String,
    var blockedReason: String? = null,
) : LorenzEvent()
