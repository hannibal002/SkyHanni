package de.hype.bingonet.shared.packets.mining

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.MiningEvents

/**
 * used to announce a mining event network wide. Can be used by both Client and Server to announce to each other.
 */
class MiningEventPacket(@JvmField val event: MiningEvents, val username: String, island: Islands) :
    AbstractPacket() {

    @JvmField
    val island: Islands

    /**
     * @param event    Event happening [(available constants)][MiningEvents]
     * @param username username of the finder
     * @param island   Island Event is happening on. Options: [Islands.DWARVEN_MINES] , [Islands.CRYSTAL_HOLLOWS]
     * @throws Exception when the Island is invalid. Can be when the island is CH but event can only be in Dwarfen Mines
     */
    init {
        require(!(island != Islands.CRYSTAL_HOLLOWS && island != Islands.DWARVEN_MINES)) { "Invalid Island!" }
        if (island == Islands.CRYSTAL_HOLLOWS) {
            require(!(event == MiningEvents.MITHRIL_GOURMAND || event == MiningEvents.RAFFLE || event == MiningEvents.GOBLIN_RAID)) { "The specified event can not happen on this Server" }
        }
        this.island = island
    }
}
