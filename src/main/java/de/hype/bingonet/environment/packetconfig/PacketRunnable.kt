package de.hype.bingonet.environment.packetconfig

import de.hype.bingonet.environment.packetconfig.AbstractPacket

internal fun interface PacketRunnable<T : AbstractPacket> {
    fun run(packet: T)

    fun parseAndRun(packet: AbstractPacket){
        @Suppress("UNCHECKED_CAST")
        run(packet as T)
    }
}

abstract class InterceptPacketInfo<T : AbstractPacket>(
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

    fun matches(packetClass: Class<*>): Boolean {
        return packetClass == clazz
    }

}
