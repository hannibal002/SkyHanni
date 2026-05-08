package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.NeuInternalName

data class BaitType(val bait: String, val internalName: NeuInternalName) {
    override fun toString(): String {
        return internalName.asString()
    }
}

