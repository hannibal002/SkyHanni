package at.hannibal2.skyhanni.data.jsonobjects.repo.neu
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuPetsJson(
    @Expose @SerializedName("pet_rarity_offset") val petRarityOffset: Map<String, Int>,
    @Expose @SerializedName("pet_levels") val basePetLeveling: List<Int>,
    @Expose @SerializedName("custom_pet_leveling") val customPetLeveling: Map<String, NeuPetData>,
    @Expose @SerializedName("pet_types") val petTypes: Map<String, String>,
    @Expose @SerializedName("id_to_display_name") val displayNameMap: Map<String, String>,
    @Expose @SerializedName("pet_item_display_name_to_id") val petItemResolution: Map<String, NeuInternalName>,
)

data class NeuPetData(
    @Expose @SerializedName("type") val type: Int? = null,
    @Expose @SerializedName("pet_levels") val petLevels: List<Int>? = null,
    @Expose @SerializedName("max_level") val maxLevel: Int? = null,
    @Expose @SerializedName("rarity_offset") val rarityOffset: Map<LorenzRarity, Int>? = null,
    @Expose @SerializedName("xp_multiplier") val xpMultiplier: Double? = null,
)
