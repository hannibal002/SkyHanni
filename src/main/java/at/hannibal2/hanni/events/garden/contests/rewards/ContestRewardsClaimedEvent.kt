package at.hannibal2.hanni.events.garden.contests.rewards

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.config.storage.Resettable
import at.hannibal2.hanni.features.garden.AnitaMedalProfit
import at.hannibal2.hanni.features.garden.CropType

class ContestRewardsClaimedEvent(val rewards: ContestRewardSet, val messages: List<String>) : HanniEvent()

data class ContestRewardSet(
    var jacobTickets: Int = 0,
    var carnivalTickets: Int = 0,
    var books: Map<CropType, Int> = emptyMap(),
    var medals: Map<AnitaMedalProfit.MedalType, Int> = emptyMap(),
    var bits: Int = 0
) : Resettable
