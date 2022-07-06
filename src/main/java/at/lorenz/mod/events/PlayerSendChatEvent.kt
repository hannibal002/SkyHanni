package at.lorenz.mod.events

import at.lorenz.mod.chat.PlayerMessageChannel


class PlayerSendChatEvent(
    val channel: PlayerMessageChannel,
    val playerName: String,
    var message: String,
    var cancelledReason: String = ""
) : LorenzEvent()