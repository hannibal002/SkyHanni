package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@KSerializable
data class NeuReforgeJson(
    @Expose val reforgeName: String,
    @Expose @SerializedName("nbtModifier") val rawNbtModifier: String? = null,
    @Expose val internalName: NeuInternalName? = null,
    @Expose @SerializedName("itemTypes") val rawItemTypes: Any,
    @Expose val requiredRarities: List<LorenzRarity>,
    @Expose val reforgeCosts: Map<LorenzRarity, Long>? = null,
    @Expose val reforgeStats: Map<LorenzRarity, SkyblockStatList>? = null,
    @Expose @SerializedName("reforgeAbility") val rawReforgeAbility: Any? = null,
) {

    val nbtModifier: String by lazy {
        rawNbtModifier ?: reforgeName.lowercase()
            .replace("[^a-z0-9\\s_-]".toRegex(), "")
            .replace("[\\s-]".toRegex(), "_")
    }
    @Suppress("UNCHECKED_CAST")
    val reforgeAbility: Map<LorenzRarity, String> by lazy {
        when (rawReforgeAbility) {
            is String -> requiredRarities.associateWith { rawReforgeAbility }

            is Map<*, *> -> (rawReforgeAbility as? Map<String, String>)?.mapKeys {
                LorenzRarity.getByNameOrError(it.key)
            }.orEmpty()

            else -> emptyMap()
        }
    }
    @Suppress("UNCHECKED_CAST")
    val itemType: Pair<String, List<NeuInternalName>> by lazy {
        when (rawItemTypes) {
            is String -> rawItemTypes.replace("/", "_AND_").uppercase() to emptyList()

            is Map<*, *> -> {
                val type = "SPECIAL_ITEMS"
                val map = rawItemTypes as? Map<String, List<String>> ?: return@lazy type to emptyList()
                val internalNames = map["internalName"]?.toInternalNames().orEmpty()
                val itemType = map["itemid"]?.map {
                    NeuItems.getInternalNamesForItemId(it.getVanillaItem() ?: return@map emptyList())
                }?.flatten().orEmpty()
                type to (internalNames + itemType)
            }

            else -> error("rawItemTypes is neither String nor Map: $rawItemTypes")
        }
    }
}
