package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.cocoon.CocoonAPI.CocoonMob
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when a new cocoon is detected in the world, carrying the cocooned mob data.
 */
@PrimaryFunction("onCocoonSpawn")
class CocoonSpawnEvent(val cocoonMob: CocoonMob) : SkyHanniEvent()
