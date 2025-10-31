package at.hannibal2.hanni.events.hypixel.modapi

import at.hannibal2.hanni.api.event.HanniEvent
import net.hypixel.data.type.ServerType

data class HypixelApiServerChangeEvent(
    val serverName: String?,
    val serverType: ServerType?,
    val lobbyName: String?,
    val mode: String?,
    val map: String?,
) : HanniEvent()
