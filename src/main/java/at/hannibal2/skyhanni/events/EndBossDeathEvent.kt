package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Sent the moment an End boss dies, before its end-of-fight summary has been read. Use this when
 * something has to react as early as possible; [EndBossFightEndEvent] carries the result but
 * arrives a moment later.
 */
@PrimaryFunction("onEndBossDeath")
class EndBossDeathEvent(val boss: EndBoss) : SkyHanniEvent()
