package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.hypixel.data.region.Environment

class HypixelJoinEvent(val environment: Environment) : LorenzEvent()
