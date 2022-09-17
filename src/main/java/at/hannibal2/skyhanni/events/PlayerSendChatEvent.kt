package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.chat.playerchat.PlayerMessageChannel
import net.minecraft.util.ChatComponentText

class PlayerSendChatEvent(
    val channel: PlayerMessageChannel,
    val name: String,
    val message: String,
    val chatComponents: MutableList<ChatComponentText>,
    var cancelledReason: String = "",
) : LorenzEvent()