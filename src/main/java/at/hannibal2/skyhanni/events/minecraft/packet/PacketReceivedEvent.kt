package at.hannibal2.skyhanni.events.minecraft.packet

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.network.protocol.Packet

class PacketReceivedEvent(val packet: Packet<*>) : CancellableSkyHanniEvent()
