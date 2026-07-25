package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onFriendAdd")
class FriendAddEvent(val playerName: String) : SkyHanniEvent()

@PrimaryFunction("onFriendRemove")
class FriendRemoveEvent(val playerName: String) : SkyHanniEvent()

@PrimaryFunction("onFriendRequestExpired")
class FriendRequestExpiredEvent(val playerName: String) : SkyHanniEvent()

@PrimaryFunction("onFriendRequestSent")
class FriendRequestSentEvent(val playerName: String) : SkyHanniEvent()

@PrimaryFunction("onFriendRequestDeclined")
class FriendRequestDeclinedEvent(val playerName: String) : SkyHanniEvent()
