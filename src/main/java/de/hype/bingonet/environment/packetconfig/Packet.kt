package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import java.util.function.Consumer

class Packet<T : AbstractPacket?>(val clazz: Class<T?>, val consumer: Consumer<T?>?) {
    val name: String
        get() = clazz.getSimpleName()
}
