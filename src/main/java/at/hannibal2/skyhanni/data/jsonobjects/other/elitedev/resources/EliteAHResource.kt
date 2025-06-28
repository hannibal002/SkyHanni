package at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.resources

import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class EliteAuctionsResponse(
    @Expose val items: Map<NeuInternalName, List<EliteAuctionItem>>,
)

enum class VariantType(val prefix: String, val enumClazz: Class<out Enum<*>>, vbAccessor: String? = null) {
    NONE("", Enum::class.java),
    RARITY("r:", LorenzRarity::class.java),
    ;

    val vbAccessor: String = vbAccessor ?: name.lowercase()
}

data class EliteLowestSet(
    @Expose val lowest: Long,
    @Expose val lowestVolume: Int,
)

data class EliteAuctionItem(
    @Expose @SerializedName("skyblockId") val internalName: NeuInternalName,
    @Expose private val variantKey: String,
    @Expose private val variedBy: Map<String, String>,
    @Expose private val lowest: Long,
    @Expose private val lowestVolume: Int,
    @Expose private val lowest3Day: Long,
    @Expose private val lowest3DayVolume: Int,
    @Expose private val lowest7Day: Long,
    @Expose private val lowest7DayVolume: Int,
) {
    val lowestSet: EliteLowestSet = EliteLowestSet(lowest, lowestVolume)
    val lowest3DaySet: EliteLowestSet = EliteLowestSet(lowest3Day, lowest3DayVolume)
    val lowest7DaySet: EliteLowestSet = EliteLowestSet(lowest7Day, lowest7DayVolume)

    private val variantType: VariantType = VariantType.entries.firstOrNull {
        variantKey.startsWith(it.prefix)
    } ?: VariantType.NONE

    @Suppress("UNCHECKED_CAST")
    fun <T : Enum<T>> getVariantAs(clazz: Class<T>): T? {
        if (variantType.enumClazz != clazz) return null
        val name = variedBy[variantType.vbAccessor] ?: return null
        val enumClazz = clazz.enumConstants
        return enumClazz.firstOrNull { it.name.equals(name, ignoreCase = true) } as? T
    }
}

