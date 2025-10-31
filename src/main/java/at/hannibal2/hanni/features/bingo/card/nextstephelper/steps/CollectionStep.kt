package at.hannibal2.hanni.features.bingo.card.nextstephelper.steps

import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.shortFormat

class CollectionStep(collectionName: String, amountNeeded: Int) :
    ProgressionStep(amountNeeded.shortFormat() + " $collectionName Collection", amountNeeded.toLong()) {

    val internalName by lazy {
        NeuInternalName.fromItemName(if (collectionName == "Mushroom") "Red Mushroom" else collectionName)
    }
}
