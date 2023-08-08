package at.hannibal2.skyhanni.features.bingo.nextstep

abstract class ProgressionStep(displayName: String, open val amountNeeded: Long, var amountHaving: Long = 0) :
    NextStep(displayName)