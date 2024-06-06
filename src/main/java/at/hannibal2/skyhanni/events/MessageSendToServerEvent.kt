package at.hannibal2.skyhanni.events

import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class MessageSendToServerEvent(
    val message: String,
    val splitMessage: List<String>,
    val originatingModContainer: ModContainer?
) : LorenzEvent()
