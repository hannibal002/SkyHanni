package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.InternalReasonConstants
import de.hype.bingonet.shared.objects.BNRole

/**
 * Gives the User feedback that the command had a problem.
 */
class InvalidCommandFeedbackPacket
/**
 * @param internalReason   for options see [InternalReasonConstants]
 * @param command          command which was executed by the client that caused the problem
 * @param displayMessage   message that shall be displayed on the client.
 * @param argument         argument in the command that caused the problem.
 * @param permissionNeeded permission required for that command / argument
 * @param userPermissions  permissions the user has.
 */(
    val internalReason: InternalReasonConstants?,
    val command: String?,
    val displayMessage: String?,
    val argument: String?,
    val permissionNeeded: String?,
    val userPermissions: Set<BNRole>?
) : AbstractPacket()
