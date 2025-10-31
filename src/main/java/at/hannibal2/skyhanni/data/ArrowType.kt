package at.hannibal2.hanni.data

import at.hannibal2.hanni.utils.NeuInternalName

data class ArrowType(val arrow: String, val internalName: NeuInternalName) {
    override fun toString(): String {
        return internalName.asString()
    }
}
