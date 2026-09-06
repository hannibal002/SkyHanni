package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired one tick after Hypixel sends a chat message announcing a teleport, for example a warp
 * command, a transfer token, warping to the own island, or visiting another player.
 *
 * Fired from `EntityMovementData`, only on SkyBlock. The teleport has not happened yet, the player
 * is usually still in the old world, and the chat message carries no destination.
 *
 * Use [IslandJoinEvent] instead when the new island needs to be known.
 */
@PrimaryFunction("onWarp")
object SkyHanniWarpEvent : SkyHanniEvent()
