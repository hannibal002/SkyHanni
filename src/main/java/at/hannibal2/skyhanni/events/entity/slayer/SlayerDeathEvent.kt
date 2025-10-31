package at.hannibal2.hanni.events.entity.slayer

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.slayer.SlayerType

class SlayerDeathEvent(val slayerType: SlayerType, val tier: Int, val owner: String?) : HanniEvent()
