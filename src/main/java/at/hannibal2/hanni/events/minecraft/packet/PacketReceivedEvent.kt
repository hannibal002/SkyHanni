package at.hannibal2.hanni.events.minecraft.packet

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import net.minecraft.network.Packet

class PacketReceivedEvent(val packet: Packet<*>) : CancellableHanniEvent()
