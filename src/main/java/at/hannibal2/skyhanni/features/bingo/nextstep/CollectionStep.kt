package at.hannibal2.skyhanni.features.bingo.nextstep

import at.hannibal2.skyhanni.utils.NumberUtil

class CollectionStep(val collectionName: String, amountNeeded: Int) :
    ProgressionStep(NumberUtil.format(amountNeeded) + " $collectionName Collection", amountNeeded.toLong())