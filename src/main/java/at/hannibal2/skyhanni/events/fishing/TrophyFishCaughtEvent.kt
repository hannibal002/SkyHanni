package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Event that is fired when a trophy fish is caught.
 *
 * the trophyFishName is the name of the trophy fish, and is NOT the [NeuInternalName] of it.
 * and rarity is the rarity of the caught trophy fish. from bronze to diamond.
 */
@PrimaryFunction("onTrophyFishCaught")
class TrophyFishCaughtEvent(val trophyFishName: String, val rarity: TrophyRarity) : SkyHanniEvent()
