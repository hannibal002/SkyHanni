package de.hype.bingonet.shared.objects

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.StatusConstants
import kotlin.properties.Delegates

open class SplashData @JvmOverloads constructor(
    open val announcer: String,
    @JvmField val locationInHub: SplashLocation,
    @JvmField var extraMessage: String?,
    @JvmField val lessWaste: Boolean,
    @JvmField val serverID: String?,
    /**
     * If null the Splash is in a private Mega → Request Invite.
     */
    @JvmField val hubSelectorData: HubSelectorData?,
    @JvmField var status: StatusConstants = StatusConstants.WAITING,
) {
    var splashId by Delegates.notNull<Int>()


    init {
        this.extraMessage = extraMessage?.replace("&", "§")
    }

    constructor(packet: SplashData) : this(
        packet.announcer,
        packet.locationInHub,
        packet.extraMessage,
        packet.lessWaste,
        packet.serverID,
        packet.hubSelectorData,
        packet.status,
    ) {
        this.splashId = packet.splashId
    }


    class HubSelectorData(hubNumber: Int, hubType: Islands) {
        @JvmField
        var hubNumber: Int

        @JvmField
        var hubType: Islands

        init {
            require(hubType == Islands.HUB || hubType == Islands.DUNGEON_HUB) { "§cInvalid hub type specified. Please only use the Suggestions!" }
            require(!(hubNumber < 1 || hubNumber > 28)) { "§cInvalid hub number specified. Must be between 1 and 28" }
            this.hubNumber = hubNumber
            this.hubType = hubType
        }
    }
}
