package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the player catches a trophy frog.
 *
 * @param trophyFrogName The clean (colourless) display name of the caught trophy frog, e.g. "Common Frog".
 * @param rarity The rarity of the caught trophy frog.
 */
@PrimaryFunction("onTrophyFrogCaught")
class TrophyFrogCaughtEvent(val trophyFrogName: String, val rarity: TrophyRarity) : SkyHanniEvent()
