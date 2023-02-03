package at.hannibal2.skyhanni.features.bingo.nextstep

import at.hannibal2.skyhanni.data.IslandType

class IslandVisitStep(val island: IslandType) : NextStep("Visit ${island.displayName}")