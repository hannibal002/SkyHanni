package at.hannibal2.skyhanni.events

import net.minecraft.util.IChatComponent

class LorenzChatEvent(val message: String, var chatComponent: IChatComponent, var blockedReason: String = "") : LorenzEvent()