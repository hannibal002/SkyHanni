package at.hannibal2.skyhanni.features.bingo.nextstep

import at.hannibal2.skyhanni.utils.NumberUtil

class CollectionStep(val itemName: String, amountNeeded: Int) :
    NextStep(NumberUtil.format(amountNeeded) + " $itemName Collection")