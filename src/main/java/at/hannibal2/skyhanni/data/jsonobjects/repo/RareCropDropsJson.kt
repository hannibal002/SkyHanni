package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class RareCropDropsJson(
    @Expose @SerializedName("special_crops") val specialCrops: Map<String, RareCropDropInfo>,
)

data class RareCropDropInfo(
    @Expose @SerializedName("armor_type") val armorType: String,
    @Expose val chance: List<Double>,
)
