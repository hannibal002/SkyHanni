package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread

@Thread(RENDER)
class BossbarUpdateEvent(val bossbar: String) : SkyHanniEvent()
