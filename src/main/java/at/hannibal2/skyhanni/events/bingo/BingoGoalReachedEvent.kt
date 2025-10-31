package at.hannibal2.hanni.events.bingo

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.bingo.card.goals.BingoGoal

class BingoGoalReachedEvent(val goal: BingoGoal) : HanniEvent()
