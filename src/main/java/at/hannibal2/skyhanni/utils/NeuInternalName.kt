package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.minutes

@JvmInline
value class NeuInternalName private constructor(private val internalName: String) {

    fun asString() = internalName

    override fun toString(): String = "internalName:$internalName"

    fun contains(other: String) = internalName.contains(other)

    fun startsWith(other: String) = internalName.startsWith(other)

    fun endsWith(other: String) = internalName.endsWith(other)

    fun replace(oldValue: String, newValue: String): NeuInternalName =
        internalName.replace(oldValue, newValue, ignoreCase = true).toInternalName()

    fun isKnownItem(): Boolean = getItemStackOrNull() != null || this == SKYBLOCK_COIN

    fun getItemCategoryOrNull(): ItemCategory? =
        categoryCache.getOrPut(this) { getItemStackOrNull()?.getItemCategoryOrNull() ?: return null }

    /**
     * This is because skyblock has special ids in commands such as /viewrecipe for items like enchanted books and pets
     */
    val skyblockCommandId: String
        get() = when {
            isPet -> internalName.split(";").first()
            isEnchantedBook && internalName.contains(";") -> {
                val (name, level) = internalName.split(";", limit = 2)
                "ENCHANTED_BOOK_${name}_$level"
            }
            else -> internalName
        }

    val isPet: Boolean
        get() = petCache.getOrPut(this) {
            PetUtils.isKnownPetInternalName(this) || (getItemCategoryOrNull() == ItemCategory.PET)
        }

    private val isEnchantedBook: Boolean
        get() = getItemStackOrNull()?.item == Items.ENCHANTED_BOOK


    companion object {

        val NONE = "NONE".toInternalName()
        val MISSING_ITEM = "MISSING_ITEM".toInternalName()

        val GEMSTONE_COLLECTION = "GEMSTONE_COLLECTION".toInternalName()
        val JASPER_CRYSTAL = "JASPER_CRYSTAL".toInternalName()
        val RUBY_CRYSTAL = "RUBY_CRYSTAL".toInternalName()
        val SKYBLOCK_COIN = "SKYBLOCK_COIN".toInternalName()
        val WISP_POTION = "WISP_POTION".toInternalName()
        val ENCHANTED_HAY_BLOCK = "ENCHANTED_HAY_BLOCK".toInternalName()
        val TIGHTLY_TIED_HAY_BALE = "TIGHTLY_TIED_HAY_BALE".toInternalName()

        fun String.toInternalName(): NeuInternalName {
            val formatted = uppercase().replace(" ", "_")
            if (formatted.any { it.equalsOneOf('§', '&', '\'') }) {
                ErrorManager.skyHanniError(
                    "Internal name found with color codes",
                    "Internal Name" to formatted, "Original String" to this,
                )
            }
            return NeuInternalName(formatted)
        }

        fun Set<String>.toInternalNames(): Set<NeuInternalName> = mapTo(mutableSetOf()) { it.toInternalName() }
        fun List<String>.toInternalNames(): List<NeuInternalName> = mapTo(mutableListOf()) { it.toInternalName() }

        private val itemNameCache = mutableMapOf<String, NeuInternalName?>()

        fun fromItemNameOrNull(itemName: String): NeuInternalName? = itemNameCache.getOrPut(itemName) {
            ItemNameResolver.getInternalNameOrNull(itemName.removeSuffix(" Pet")) ?: getCoins(itemName)
        }

        fun fromItemNameOrInternalName(itemName: String): NeuInternalName = fromItemNameOrNull(itemName) ?: itemName.toInternalName()

        private val categoryCache = TimeLimitedCache<NeuInternalName, ItemCategory>(10.minutes)

        private val petCache: TimeLimitedCache<NeuInternalName, Boolean> = TimeLimitedCache(10.minutes)

        private fun getCoins(itemName: String): NeuInternalName? = when {
            isCoins(itemName) -> SKYBLOCK_COIN
            else -> null
        }

        private val coinNames = setOf(
            "coin", "coins",
            "skyblock coin", "skyblock coins",
            "skyblock_coin", "skyblock_coins",
        )

        private fun isCoins(itemName: String): Boolean = itemName.lowercase() in coinNames

        fun fromItemName(itemName: String): NeuInternalName = fromItemNameOrNull(itemName) ?: run {
            val name = "itemName:$itemName"
            ItemUtils.addMissingRepoItem(name, "Could not find internal name for $name")
            MISSING_ITEM
        }
    }
}
