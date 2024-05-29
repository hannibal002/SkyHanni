package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity

// trophyFishName is NO Neu Internal Name
class TrophyFishCaughtEvent(val trophyFishName: String, val rarity: TrophyRarity) : LorenzEvent()
