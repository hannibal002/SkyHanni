package at.hannibal2.skyhanni.features.bingo.nextstep

class ItemsStep(displayName: String, val itemName: String, amountNeeded: Long, val variants: Map<String, Int>) :
    ProgressionStep(displayName, amountNeeded)