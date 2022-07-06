package at.lorenz.mod.events

import net.minecraft.util.IChatComponent

class LorenzChatEvent(val message: String, val chatComponent: IChatComponent, var blockedReason: String = "") : LorenzEvent()