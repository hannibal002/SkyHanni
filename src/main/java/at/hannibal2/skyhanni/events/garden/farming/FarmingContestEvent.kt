package at.hannibal2.hanni.events.garden.farming

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.garden.CropType
import at.hannibal2.hanni.features.garden.contest.FarmingContestPhase

class FarmingContestEvent(val crop: CropType, val phase: FarmingContestPhase) : HanniEvent()
