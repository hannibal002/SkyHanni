package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.NEUInternalName

data class ArrowType(val arrow: String, val internalName: NEUInternalName) {
    override fun toString() = internalName.asString()

    override fun hashCode(): Int {
        var result = arrow.hashCode()
        result = 31 * result + internalName.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrowType

        if (arrow != other.arrow) return false
        if (internalName != other.internalName) return false

        return true
    }
}
