package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.system.ModInstance
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class MessageSendToServerEvent(
    val message: String,
    val splitMessage: List<String>,
    val originatingModContainer: ModInstance?
) : LorenzEvent()
