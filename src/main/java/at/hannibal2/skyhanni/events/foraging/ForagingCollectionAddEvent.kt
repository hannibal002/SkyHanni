package at.hannibal2.skyhanni.events.foraging

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.foraging.ForagingLogType

class ForagingCollectionAddEvent(val logType: ForagingLogType, val amount: Long) : SkyHanniEvent()
