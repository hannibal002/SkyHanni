package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.chat.PlayerMessageChannel

class PlayerSendChatEvent(
    val channel: PlayerMessageChannel,
    val playerName: String,
    var message: String,
    var cancelledReason: String = ""
) : LorenzEvent()