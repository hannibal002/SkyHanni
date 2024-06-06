package at.hannibal2.skyhanni.data.jsonobjects.repo.neu;

import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuReforgeStoneJson(
    @Expose val internalName: NEUInternalName,
    @Expose val reforgeName: String,
    @Expose @SerializedName("itemTypes") val rawItemTypes: Any,
    @Expose val requiredRarities: List<LorenzRarity>,
    @Expose val reforgeCosts: Map<LorenzRarity, Long>,
    @Expose val reforgeStats: Map<LorenzRarity, Map<String, Double>>,
    @Expose @SerializedName("reforgeAbility") val rawReforgeAbility: Any?,
) {

    private lateinit var reforgeAbilityField: Map<LorenzRarity, String>

    val reforgeAbility
        get() = if (this::reforgeAbilityField.isInitialized) reforgeAbilityField
        else {
            reforgeAbilityField = when (this.rawReforgeAbility) {
                is String -> {
                    this.requiredRarities.associateWith { this.rawReforgeAbility }
                }

                is Map<*, *> -> (this.rawReforgeAbility as? Map<String, String>)?.mapKeys {
                    LorenzRarity.valueOf(
                        it.key.uppercase().replace(" ", "_")
                    )
                } ?: emptyMap()

                else -> emptyMap()
            }
            reforgeAbilityField
        }

    /* used in ReforgeAPI which isn't in beta yet
        val itemType: Pair<String, List<NEUInternalName>> by lazy {
            val any = this.rawItemTypes
            return@lazy when (any) {
                is String -> {
                    any.replace("/", "_AND_").uppercase() to emptyList()
                }

                is Map<*, *> -> {
                    val type = "SPECIAL_ITEMS"
                    val map = any as? Map<String, List<String>> ?: return@lazy type to emptyList()
                    val internalNames = map["internalName"]?.map { it.asInternalName() } ?: emptyList()
                    val itemType = map["itemid"]?.map {
                        NEUItems.getInternalNamesForItemId(Item.getByNameOrId(it))
                    }?.flatten()
                        ?: emptyList()
                    type to (internalNames + itemType)
                }

                else -> throw IllegalStateException()
            }
        }
*/
}

