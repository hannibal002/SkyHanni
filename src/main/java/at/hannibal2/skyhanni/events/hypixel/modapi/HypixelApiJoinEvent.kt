package at.hannibal2.skyhanni.events.hypixel.modapi

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread

@Thread(RENDER)
data class HypixelApiJoinEvent(val alpha: Boolean) : SkyHanniEvent()
