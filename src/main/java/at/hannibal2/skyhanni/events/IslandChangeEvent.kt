package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.hannimodule.PrimaryFunction

@PrimaryFunction("onIslandChange")
class IslandChangeEvent(val newIsland: IslandType, val oldIsland: IslandType) : HanniEvent()
