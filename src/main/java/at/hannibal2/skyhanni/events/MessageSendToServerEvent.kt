package at.hannibal2.skyhanni.events

import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class MessageSendToServerEvent(val message: String) : LorenzEvent()
