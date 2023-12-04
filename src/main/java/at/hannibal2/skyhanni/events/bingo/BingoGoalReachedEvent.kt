package at.hannibal2.skyhanni.events.bingo

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.bingo.card.BingoGoal

class BingoGoalReachedEvent(val goal: BingoGoal) : LorenzEvent()
