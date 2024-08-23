package at.hannibal2.skyhanni.utils

class NEUInternalName private constructor(private val internalName: String) {

    companion object {

        private val map = mutableMapOf<String, NEUInternalName>()

        val NONE = "NONE".asInternalName()
        val MISSING_ITEM = "MISSING_ITEM".asInternalName()

        val JASPER_CRYSTAL = "JASPER_CRYSTAL".asInternalName()
        val RUBY_CRYSTAL = "RUBY_CRYSTAL".asInternalName()
        val SKYBLOCK_COIN = "SKYBLOCK_COIN".asInternalName()
        val WISP_POTION = "WISP_POTION".asInternalName()

        fun String.asInternalName(): NEUInternalName {
            val internalName = uppercase().replace(" ", "_")
            return map.getOrPut(internalName) { NEUInternalName(internalName) }
        }

        fun fromItemNameOrNull(itemName: String): NEUInternalName? =
            ItemNameResolver.getInternalNameOrNull(itemName.removeSuffix(" Pet"))

        fun fromItemName(itemName: String): NEUInternalName = fromItemNameOrNull(itemName) ?: run {
            val name = "itemName:$itemName"
            ItemUtils.addMissingRepoItem(name, "Could not find internal name for $name")
            return NEUInternalName.MISSING_ITEM
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

    fun equals(other: String) = internalName == other

    fun contains(other: String) = internalName.contains(other)

    fun startsWith(other: String) = internalName.startsWith(other)

    fun endsWith(other: String) = internalName.endsWith(other)

    fun replace(oldValue: String, newValue: String) =
        internalName.replace(oldValue.uppercase(), newValue.uppercase()).asInternalName()
}
