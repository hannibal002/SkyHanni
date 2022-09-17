package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.chat.playerchat.PlayerMessageChannel
import net.minecraft.util.ChatComponentText

class PlayerSendChatEvent(
    val channel: PlayerMessageChannel,
    val name: String,
    var message: String,
    var cancelledReason: String = "",
) : LorenzEvent()