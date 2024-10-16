package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NEUPetsJson(
    @Expose @SerializedName("pet_levels") val petLevels: List<Int>,
    @Expose @SerializedName("custom_pet_leveling") val customPetLeveling: JsonObject,
    @Expose @SerializedName("pet_rarity_offset") val petRarityOffset: JsonObject,
)
