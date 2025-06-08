package at.hannibal2.skyhanni.events.minecraft.packet

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.network.Packet

class PacketSentEvent(val packet: Packet<*>) : CancellableSkyHanniEvent() {
    fun findOriginatingModCall(skipSkyhanni: Boolean = false): StackTraceElement? {
        val nonMinecraftOriginatingStack = Thread.currentThread().stackTrace
            // Skip calls before the event is being called
            //#if MC < 1.21
            .dropWhile { it.className != "net.minecraft.client.network.NetHandlerPlayClient" }
            //#else
            //$$ .dropWhile { it.className != "net.minecraft.network.ClientConnection" }
            //#endif
            // Limit the remaining callstack until only the main entrypoint to hide the relauncher
            .takeWhile { !it.className.endsWith(".Main") }
            // Drop minecraft or skyhanni call frames
            .dropWhile {
                it.className.startsWith("net.minecraft.") ||
                    (skipSkyhanni && it.className.startsWith("at.hannibal2.skyhanni."))
            }
            .firstOrNull()
        return nonMinecraftOriginatingStack
    }
}
