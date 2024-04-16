package at.hannibal2.skyhanni.events

data class PartyChatEvent(
    val author: String,
    val text: String,
    val trigger: LorenzChatEvent,
) : LorenzEvent()
