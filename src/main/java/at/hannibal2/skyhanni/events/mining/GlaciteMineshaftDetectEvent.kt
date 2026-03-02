package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection

class GlaciteMineshaftDetectEvent(val type: MineshaftDetection.MineshaftType) : SkyHanniEvent()
