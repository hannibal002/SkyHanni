package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.hannimodule.PrimaryFunction

@PrimaryFunction("onProfileJoin")
class ProfileJoinEvent(val name: String) : HanniEvent()
