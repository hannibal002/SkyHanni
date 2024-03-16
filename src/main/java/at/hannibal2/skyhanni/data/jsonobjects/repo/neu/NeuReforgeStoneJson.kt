package at.hannibal2.skyhanni.data.jsonobjects.repo.neu;

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuReforgeStoneJson(

    @Expose
    val internalName: String,

    @Expose
    val reforgeName: String,

    @Expose
    @SerializedName("itemTypes")
    val rawItemTypes: Any,

    @Expose
    val requiredRarities: List<String>,

    @Expose
    val reforgeCosts: Map<String, Long>,

    @Expose
    val reforgeStats: Map<String, Map<String, Double>>,

    @Expose
    @SerializedName("reforgeAbility")
    val rawReforgeAbility: Any?,
) {
    /* used in ReforgeAPI which isn't in beta yet
        lateinit var reforgeAbility: Map<String, String>

        lateinit var itemType: Pair<String, List<NEUInternalName>>

        fun init() {
            reforgeAbility = when (this.rawReforgeAbility) {
                is String -> {
                    this.requiredRarities.associateWith { this.rawReforgeAbility }
                }

                is Map<*, *> -> this.rawReforgeAbility as? Map<String, String> ?: emptyMap()
                else -> emptyMap()
            }
            itemType = run {
                val any = this.rawItemTypes
                return@run when (any) {
                    is String -> {
                        any.replace("/", "_AND_").uppercase() to emptyList()
                    }

                    is Map<*, *> -> {
                        val type = "SPECIAL_ITEMS"
                        val map = any as? Map<String, List<String>> ?: return@run type to emptyList()
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
        } */
}
