package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection

/**
 * Fired once when the player enters a Glacite Mineshaft and the mineshaft type is identified from the scoreboard.
 *
 * @param type The detected mineshaft type.
 */
class GlaciteMineshaftDetectEvent(val type: MineshaftDetection.MineshaftType) : SkyHanniEvent()
