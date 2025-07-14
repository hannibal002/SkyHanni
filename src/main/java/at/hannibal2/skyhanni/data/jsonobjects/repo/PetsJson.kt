package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PetsJson(
    @Expose val skins: PetsJsonSkins
)

data class PetsJsonSkins(
    @Expose @SerializedName("game_variants") val gameVariants: Map<String, Set<NeuInternalName>>
)
