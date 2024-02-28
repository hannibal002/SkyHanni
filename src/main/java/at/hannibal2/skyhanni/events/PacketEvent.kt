package at.hannibal2.skyhanni.events

import net.minecraft.network.Packet
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
/**
 * Note: This event is async and may not be executed on the main minecraft thread.
 */
abstract class PacketEvent : LorenzEvent() {

    abstract val direction: Direction
    abstract val packet: Packet<*>

    /**
     * Note: This event is async and may not be executed on the main minecraft thread.
     */
    data class ReceiveEvent(override val packet: Packet<*>) : PacketEvent() {

        override val direction = Direction.INBOUND
    }

    /**
     * Note: This event is async and may not be executed on the main minecraft thread.
     */
    data class SendEvent(override val packet: Packet<*>) : PacketEvent() {

        override val direction = Direction.OUTBOUND

        fun findOriginatingModCall(skipSkyhanni: Boolean = false): StackTraceElement? {
            val nonMinecraftOriginatingStack = Thread.currentThread().stackTrace
                // Skip calls before the event is being called
                .dropWhile { it.className != "net.minecraft.client.network.NetHandlerPlayClient" }
                // Limit the remaining callstack until only the main entrypoint to hide the relauncher
                .takeWhile { !it.className.endsWith(".Main") }
                // Drop minecraft or skyhanni call frames
                .dropWhile { it.className.startsWith("net.minecraft.") || (skipSkyhanni && it.className.startsWith("at.hannibal2.skyhanni.")) }
                .firstOrNull()
            return nonMinecraftOriginatingStack
        }
    }

    enum class Direction {
        INBOUND, OUTBOUND
    }
}
