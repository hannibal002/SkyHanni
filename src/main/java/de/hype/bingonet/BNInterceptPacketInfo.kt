package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

internal fun interface PacketRunnable<T : AbstractPacket> {
    fun run(packet: T)
}

abstract class BNInterceptPacketInfo<T : AbstractPacket>(
    val clazz: Class<T>,
    val cancelPacket: Boolean,
    val blockIntercepts: Boolean,
    val ignoreIfIntercepted: Boolean,
    /**
     * block execution for completion is used whether the checks shall continue or the connection shall be paused until this is completed.
     */
    val blockExecutionForCompletion: Boolean
) : PacketRunnable<T> {
    val replyId: Long = -1

    fun matches(packetClass: Class<T>): Boolean {
        return packetClass == clazz
    }

}
