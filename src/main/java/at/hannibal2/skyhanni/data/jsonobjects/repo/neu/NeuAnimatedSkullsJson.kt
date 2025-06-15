package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuAnimatedSkullsJson(
    @Expose val skins: Map<String, AnimatedSkinJson>,
    @Expose @SerializedName("pet_skin_variant") val petSkinVariants: Map<NeuInternalName, List<String>>,
    @Expose @SerializedName("pet_skin_nbt_name") val petSkinNbtNames: List<String>,
)

data class AnimatedSkinJson(
    @Expose val ticks: Int,
    @Expose val textures: List<String>,
)
