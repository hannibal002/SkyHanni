package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class RequestUserInfoPacket : AbstractPacket {
    val requestUpToDateData: Boolean
    val bbUserId: Int?
    val mcUsername: String?
    val dcUserId: Long?
    val cardCount: Int?
    val bingoPoints: Int?
    val displayPrefix: String?

    var roles: MutableList<String>

    constructor(
        requestUpToDateData: Boolean,
        bbUserId: Int?,
        mcUsername: String?,
        dcUserId: Long?,
        cardCount: Int?,
        bingoPoints: Int?,
        displayPrefix: String?,
        roles: MutableList<String>
    ) : super(1, 1) {
        this.requestUpToDateData = requestUpToDateData
        this.bbUserId = bbUserId
        this.mcUsername = mcUsername
        this.dcUserId = dcUserId
        this.cardCount = cardCount
        this.bingoPoints = bingoPoints
        this.displayPrefix = displayPrefix
        this.roles = roles
    }

    constructor(
        requestUpToDateData: Boolean,
        bbUserId: Int?,
        mcUsername: String?,
        dcUserId: Long?,
        roles: MutableList<String>
    ) : super(1, 1) {
        this.requestUpToDateData = requestUpToDateData
        this.bbUserId = bbUserId
        this.mcUsername = mcUsername
        this.dcUserId = dcUserId
        this.cardCount = null
        this.bingoPoints = null
        this.displayPrefix = null
        this.roles = roles
    }

    fun hasRole(role: String?): Boolean {
        return roles.contains(role)
    }
}
