package at.hannibal2.skyhanni.events.bingo

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal

class BingoGoalReachedEvent(val goal: BingoGoal) : LorenzEvent()
