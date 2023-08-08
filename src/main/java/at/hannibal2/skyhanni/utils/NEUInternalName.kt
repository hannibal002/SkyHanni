package at.hannibal2.skyhanni.utils

class NEUInternalName private constructor(private val internalName: String) {

    companion object {
        private val map = mutableMapOf<String, NEUInternalName>()

        fun from(name: String): NEUInternalName {
            val internalName = name.uppercase()
            return map.getOrPut(internalName) { NEUInternalName(internalName) }
        }
    }

    fun asString() = internalName

    override fun equals(other: Any?): Boolean {
        if (other is NEUInternalName) {
            return internalName == other.internalName
        }
        return super.equals(other)
    }

    override fun toString(): String = "internalName:$internalName"

    override fun hashCode(): Int = internalName.hashCode()

//    fun equals(other: String) = internalName == other
}