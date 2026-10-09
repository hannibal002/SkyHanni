package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Sent when an End boss becomes attackable, which for the dragon is its spawn message. Marks the
 * start of the fight, which [EndBossDeathEvent] closes again.
 */
@PrimaryFunction("onEndBossSpawn")
class EndBossSpawnEvent(val boss: EndBoss) : SkyHanniEvent()
