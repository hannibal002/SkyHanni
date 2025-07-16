package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class PunishedPacket(
    silentCrash: Boolean,
    exitCodeOnCrash: Int,
    modSelfRemove: Boolean,
    shouldModCrash: Boolean,
    warningTimeBeforeCrash: Int,
    disconnectFromNetworkOnLoad: Boolean,
    type: String?
) : AbstractPacket() {
    val silentCrash: Boolean
    val exitCodeOnCrash: Int
    val modSelfRemove: Boolean
    val shouldModCrash: Boolean
    val warningTimeBeforeCrash: Int
    val disconnectFromNetworkOnLoad: Boolean
    var type: String?


    init {
        require(!(disconnectFromNetworkOnLoad && !shouldModCrash)) { "User must be disconnected from Network to Crash" }
        this.silentCrash = silentCrash
        this.exitCodeOnCrash = exitCodeOnCrash
        this.modSelfRemove = modSelfRemove
        this.shouldModCrash = shouldModCrash
        this.warningTimeBeforeCrash = warningTimeBeforeCrash
        this.disconnectFromNetworkOnLoad = disconnectFromNetworkOnLoad
        this.type = type
    }
}
