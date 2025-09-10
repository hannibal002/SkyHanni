package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.compat.getVanillaItem
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuReforgeJson(
    @Expose val reforgeName: String,
    @Expose @SerializedName("nbtModifier") val rawNbtModifier: String?,
    @Expose val internalName: NeuInternalName?,
    @Expose @SerializedName("itemTypes") val rawItemTypes: Any,
    @Expose val requiredRarities: List<LorenzRarity>,
    @Expose val reforgeCosts: Map<LorenzRarity, Long>?,
    @Expose val reforgeStats: Map<LorenzRarity, SkyblockStatList>?,
    @Expose @SerializedName("reforgeAbility") val rawReforgeAbility: Any?,
) {

    // NOTE: Lateinit fields are used here because `by lazy` causes errors
    private lateinit var nbtModifierField: String
    private lateinit var reforgeAbilityField: Map<LorenzRarity, String>
    private lateinit var itemTypeField: Pair<String, List<NeuInternalName>>

    val nbtModifier: String
        get() {
            if (!this::nbtModifierField.isInitialized) {
                nbtModifierField = rawNbtModifier ?: reforgeName
                    .lowercase()
                    .replace("[^a-z0-9\\s_-]".toRegex(), "")
                    .replace("[\\s-]".toRegex(), "_")
            }
            return nbtModifierField
        }

    val reforgeAbility: Map<LorenzRarity, String>
        get() {
            if (!this::reforgeAbilityField.isInitialized) {
                reforgeAbilityField = when (rawReforgeAbility) {
                    is String -> requiredRarities.associateWith { rawReforgeAbility }

                    is Map<*, *> -> (rawReforgeAbility as? Map<String, String>)?.mapKeys {
                        LorenzRarity.getByNameOrError(it.key)
                    }.orEmpty()

                    else -> emptyMap()
                }
            }
            return reforgeAbilityField
        }

    val itemType: Pair<String, List<NeuInternalName>>
        get() {
            if (!this::itemTypeField.isInitialized) {
                itemTypeField = when (rawItemTypes) {
                    is String -> rawItemTypes.replace("/", "_AND_").uppercase() to emptyList()

                    is Map<*, *> -> {
                        val type = "SPECIAL_ITEMS"
                        val map = rawItemTypes as? Map<String, List<String>> ?: return type to emptyList()
                        val internalNames = map["internalName"]?.toInternalNames().orEmpty()
                        val itemType = map["itemid"]?.map {
                            NeuItems.getInternalNamesForItemId(it.getVanillaItem() ?: return@map emptyList())
                        }?.flatten().orEmpty()
                        type to (internalNames + itemType)
                    }

                    else -> error("rawItemTypes is neither String nor Map: $rawItemTypes")
                }
            }
            return itemTypeField
        }
}
