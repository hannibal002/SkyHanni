package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired after the client switched to a different world, meaning the [net.minecraft.client.multiplayer.ClientLevel]
 * instance was replaced.
 *
 * Fired on the main client thread via the Fabric `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE` callback.
 * On Hypixel every server switch replaces the world, so this fires on every island change as well.
 *
 * This is the standard event for resetting world bound state, for example cached entities, positions or timers.
 *
 * Do not use this when the concrete island matters. This event carries no [IslandType] and also fires for world
 * changes that are not island changes. Use [IslandJoinEvent] or [IslandLeaveEvent] instead.
 *
 * @see IslandJoinEvent
 * @see IslandLeaveEvent
 */
@PrimaryFunction("onWorldChange")
object WorldChangeEvent : SkyHanniEvent()
