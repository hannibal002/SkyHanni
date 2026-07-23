package at.hannibal2.skyhanni.events.kuudra

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraTier

/**
 * Fired when a Kuudra boss is defeated.
 *
 * Posted when the "KUUDRA DOWN!" message appears in chat.
 *
 * [kuudraTier] holds the tier of the completed run.
 */
class KuudraCompleteEvent(val kuudraTier: KuudraTier) : SkyHanniEvent()
