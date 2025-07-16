package de.hype.bingonet.shared.packets.function.secretbingo

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class GoalSolutionReportedPacket(//TODO add description report from users.
    val goalName: String, val goalId: String, val solution: String, val verified: Boolean
) : AbstractPacket()
