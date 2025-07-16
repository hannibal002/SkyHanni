package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket


/**
 * Message in the Bingo Net Environment. Shared Discord and Mod in-game
 */
class BingoChatMessagePacket
/**
 * @param prefix      the prefix the sender has.
 * @param username    the username the sender has. (In game name)
 * @param message     message
 * @param bingo_cards the count of cards the user has.
 */(val prefix: String?, val username: String, @JvmField val message: String, val bingo_cards: Int) :
    AbstractPacket()
