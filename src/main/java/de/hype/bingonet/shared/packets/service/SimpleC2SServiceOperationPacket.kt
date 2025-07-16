package de.hype.bingonet.shared.packets.service

/**
 * Used when the Service is full and the Service can be started
 */
class SimpleC2SServiceOperationPacket(val operation: ClientServiceOperation?, val serviceId: Int) {
    enum class ClientServiceOperation(val requiresHoster: Boolean) {
        START(true),
        CLOSE_SERVICE(true),
        READY_ANSWER(false),
        JOIN(false),
        LEAVE(false),
        ;

        fun requiresHoster(): Boolean {
            return requiresHoster
        }
    }
}

