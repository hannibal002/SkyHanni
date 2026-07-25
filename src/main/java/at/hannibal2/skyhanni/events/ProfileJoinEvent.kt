package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the player joins or switches to a SkyBlock profile.
 *
 * @param name The lowercase name of the profile that was joined.
 */
@PrimaryFunction("onProfileJoin")
class ProfileJoinEvent(val name: String) : SkyHanniEvent()
