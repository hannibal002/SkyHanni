package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class CompletedGoalPacket
/**
 * @param username        username. Filled by the server
 * @param name            Goal name. Filled by the server
 * @param skyblockId      Bingo goal id (Hypixel id)
 * @param completionType  [de.hype.bingonet.server.apibingocode.abstractbase.BingoGoal][CompletionType.GOAL] or [Card][CompletionType.CARD]
 * @param lore            Bingo BingoGoal Description /
 * @param progress        progress on the Card with how many Goals Completed. -1 For Unknown. In case of Card the Amount that the user Already completed
 * @param shouldBroadcast allows you to tell the server whether you want this info to be broadcast to the other clients
 */(
    var username: String,
    @JvmField var name: String,
    @JvmField var skyblockId: String,
    var lore: String,
    @JvmField var completionType: CompletionType,
    var progress: Int,
    @JvmField var shouldBroadcast: Boolean
) : AbstractPacket() {
    enum class CompletionType {
        CARD,
        GOAL
    }
}

