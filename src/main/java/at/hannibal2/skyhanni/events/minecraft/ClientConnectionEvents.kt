package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onDisconnect")
object ClientDisconnectEvent : SkyHanniEvent()

// fires on singleplayer and multiplayer
@PrimaryFunction("onConnect")
object ClientConnectEvent : SkyHanniEvent()
