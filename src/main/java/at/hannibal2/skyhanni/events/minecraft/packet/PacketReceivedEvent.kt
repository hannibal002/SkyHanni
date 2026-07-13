package at.hannibal2.skyhanni.events.minecraft.packet

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.network.protocol.Packet

/**
 * Fired when a packet is received from the server.
 *
 * **Fired on the Netty network thread. This event is asynchronous.**
 * Handlers must not access or modify game state without proper thread synchronization.
 *
 * `BundlePacket`s are automatically unwrapped: each contained sub-packet fires its own individual event.
 *
 * @param packet the received packet
 */
class PacketReceivedEvent(val packet: Packet<*>) : SkyHanniEvent()
