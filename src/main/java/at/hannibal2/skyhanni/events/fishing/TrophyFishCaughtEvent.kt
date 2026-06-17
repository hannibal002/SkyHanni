package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

// trophyFishName is NO Neu Internal Name
@PrimaryFunction("onTrophyFishCaught")
class TrophyFishCaughtEvent(val trophyFishName: String, val rarity: TrophyRarity) : SkyHanniEvent()
