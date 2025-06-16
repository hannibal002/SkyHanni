package at.hannibal2.skyhanni.events.minecraft.packet

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.network.Packet

class PacketSentEvent(val packet: Packet<*>) : CancellableSkyHanniEvent() {

    fun findOriginatingModCall(skipSkyhanni: Boolean = false): StackTraceElement? {
        return Thread.currentThread().stackTrace
            // Skip calls before the event is being called
            .dropWhile { !isNetworkHandlerClass(it.className) }
            // Limit the remaining callstack until only the main entrypoint to hide the relauncher
            .takeWhile { !it.className.endsWith(".Main") }
            // Drop minecraft or skyhanni call frames
            .dropWhile {
                startsWithMinecraft(it.className) || (skipSkyhanni && it.className.startsWith("at.hannibal2.skyhanni."))
            }
            .firstOrNull()
    }

    companion object {

        //#if MC < 1.21
        private fun isNetworkHandlerClass(className: String) = className == "net.minecraft.client.network.NetHandlerPlayClient"
        //#else
        //$$ private val networkClassName = net.minecraft.client.network.ClientPlayNetworkHandler::class.java.name
        //$$ private fun isNetworkHandlerClass(className: String) = className == networkClassName
        //#endif

        private fun startsWithMinecraft(string: String): Boolean {
            //#if MC < 1.21
            return string.startsWith("net.minecraft.")
            //#else
            //$$ return string.startsWith("net.minecraft.") || string.startsWith("com.mojang.") || string.startsWith("org.lwjgl.")
            //#endif
        }
    }
}
