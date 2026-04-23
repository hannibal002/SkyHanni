package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.cocoon.CocoonAPI.CocoonMob
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onCocoonSpawn")
class CocoonSpawnEvent(val cocoonMob: CocoonMob) : SkyHanniEvent()
