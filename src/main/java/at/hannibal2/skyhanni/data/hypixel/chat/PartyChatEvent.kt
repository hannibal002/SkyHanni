package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzEvent

class PartyChatEvent(
    val author: String,
    val text: String,
    val trigger: LorenzChatEvent,
) : LorenzEvent()
