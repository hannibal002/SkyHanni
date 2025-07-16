package de.hype.bingonet.shared.constants

/**
 * Allowed party operations.
 *
 *
 * The available enums are:
 *
 * [.INVITE]: Represents the type for sending party invitations.
 * [.ACCEPT]: Represents the type for accepting party invitations.
 * [.DISBAND]: Represents the type for disbanding a party.
 * [.KICK]: Represents the type for kicking a user from the party.
 * [.JOIN]: Represents the type for joining a party.
 * [.TRANSFER]: Represents the type for transferring party ownership.
 * [.LEAVE]: Represents the type for leaving a party.
 * [.PROMOTE]: Represents the type for promoting a user in the party.
 *
 */
enum class PartyConstants {
    INVITE,
    JOIN,
    TRANSFER,
    ACCEPT,
    DISBAND,
    LEAVE,
    PROMOTE,
    WARP,
    KICK
}
