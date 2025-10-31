package at.hannibal2.hanni.events.fishing

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.fishing.trophy.TrophyRarity

// trophyFishName is NO Neu Internal Name
class TrophyFishCaughtEvent(val trophyFishName: String, val rarity: TrophyRarity) : HanniEvent()
