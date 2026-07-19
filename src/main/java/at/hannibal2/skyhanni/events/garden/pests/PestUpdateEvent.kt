package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * Fired when the pest state of any garden plot changes.
 *
 * This includes pest spawns, kills, and corrections from the scoreboard or tab list.
 */
object PestUpdateEvent : SkyHanniEvent()
