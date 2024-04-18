package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.LorenzEvent

class SystemMessageEvent(
    val message: String,
    var blockedReason: String? = null,
) : LorenzEvent()
