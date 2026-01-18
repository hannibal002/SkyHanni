package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.cocoon.CocoonAPI.CocoonMob

@Suppress("UnusedPrivateProperty")
class CocoonSpawnEvent(val cocoonMob: CocoonMob) : SkyHanniEvent()
