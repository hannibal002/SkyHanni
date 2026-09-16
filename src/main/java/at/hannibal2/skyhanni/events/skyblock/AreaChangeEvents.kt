package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the area name shown on the SkyBlock scoreboard changes.
 *
 * This event is a fallback. Almost always prefer [GraphAreaChangeEvent] instead, which is based on the island graph
 * data. That data is maintained in the SkyHanni repo, so it covers area borders Hypixel does not map out as clearly on the scoreboard,
 * and it can be corrected without a mod update. Only use this event when graph data is not usable in the given
 * scenario, for example on islands without graph data, or when the scoreboard name itself is the value that is needed.
 *
 * Keep the usage of this event inside backend classes in the data or api packages. Feature classes should not listen
 * to it directly.
 *
 * @param area the area name currently shown on the scoreboard.
 * @param previousArea the area name shown before this change, or null when no area was known yet.
 */
@PrimaryFunction("onScoreboardAreaChange")
class ScoreboardAreaChangeEvent(val area: String, val previousArea: String?) : SkyHanniEvent()

/**
 * Fired when the player enters a different area, detected from the closest area node in the island graph.
 *
 * This is the preferred way to react to area changes and is fair game for feature classes. Use it over
 * [ScoreboardAreaChangeEvent] unless the graph data cannot answer the question at hand.
 *
 * The event is only posted when the area name actually changed. Being outside of every known area is reported as
 * AreaNode.NO_AREA and is a normal state, not an error. On a world change the area is reset to AreaNode.NO_AREA,
 * which posts the event unless the player was already outside of every area.
 *
 * If only the current area is needed, without reacting to the change itself, use SkyBlockUtils.graphArea instead of
 * listening to this event.
 *
 * @param area the name of the area the player is now in, or AreaNode.NO_AREA when inside no known area.
 * @param previousArea the area the player was in before this change, empty before the first area of the session has
 *   been detected.
 * @param onlyInternal true when this change should not be surfaced to the user, either because the area is hidden by
 *   the area list config or because the area was reset on a world change. Logic that only cares about where the player
 *   is should ignore this flag, only user facing displays of the current area should respect it.
 */
@PrimaryFunction("onAreaChange")
class GraphAreaChangeEvent(val area: String, val previousArea: String?, val onlyInternal: Boolean) : SkyHanniEvent()
