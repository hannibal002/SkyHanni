package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuAttributeShardJson(
    @Expose @SerializedName("attribute_levelling") val attributeLevelling: Map<LorenzRarity, List<Int>>,
    @Expose @SerializedName("unconsumable_attributes") val unconsumableAttributes: List<String>,
    @Expose val attributes: List<NeuAttributeShardData>,
)

data class NeuAttributeShardData(
    @Expose val bazaarName: NeuInternalName,
    @Expose val displayName: String,
    @Expose val rarity: LorenzRarity,
    @Expose val internalName: NeuInternalName,
    @Expose val abilityName: String = "",
    @Expose val family: List<String>,
    @Expose val alignment: String,
)
