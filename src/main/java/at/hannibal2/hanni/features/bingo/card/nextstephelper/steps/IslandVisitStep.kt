package at.hannibal2.hanni.features.bingo.card.nextstephelper.steps

import at.hannibal2.hanni.data.IslandType

class IslandVisitStep(val island: IslandType) : NextStep("Visit ${island.displayName}")
