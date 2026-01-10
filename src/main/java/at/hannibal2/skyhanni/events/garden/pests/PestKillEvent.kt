package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.pests.PestType

class PestKillEvent(val pestType: PestType) : SkyHanniEvent()
