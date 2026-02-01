package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.network.protocol.Packet

object ConnectionHook {

    @JvmStatic
    fun errorBundle(packet: Packet<*>) {
        ErrorManager.logErrorStateWithData(
            "Unable to handle packet",
            "Unable to cancel bundle packet",
            "packet" to packet
        )
    }
}
