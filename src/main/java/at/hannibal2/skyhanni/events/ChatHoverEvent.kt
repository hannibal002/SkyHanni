package at.hannibal2.skyhanni.events

import net.minecraft.event.HoverEvent.Action
import net.minecraft.util.IChatComponent

class ChatHoverEvent(var action: Action, var component: IChatComponent) : LorenzEvent()
