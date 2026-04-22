package at.hannibal2.skyhanni.events.kuudra

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when a Kuudra boss is defeated.
 *
 * Posted when the "KUUDRA DOWN!" message appears in chat.
 *
 * [kuudraTier] holds the tier of the completed run (1 = basic, 2 = hot, 3 = burning, 4 = fiery, 5 = infernal).
 */
@PrimaryFunction("onKuudraComplete")
class KuudraCompleteEvent(val kuudraTier: Int) : SkyHanniEvent()
