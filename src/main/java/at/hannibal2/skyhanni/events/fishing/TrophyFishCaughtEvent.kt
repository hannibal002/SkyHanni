package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the player catches a trophy fish.
 *
 * @param trophyFishName The internal name of the caught trophy fish, not a NEU Internal Name.
 * @param rarity The rarity of the caught trophy fish.
 */
@PrimaryFunction("onTrophyFishCaught")
class TrophyFishCaughtEvent(val trophyFishName: String, val rarity: TrophyRarity) : SkyHanniEvent()
