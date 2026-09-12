package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class MiningJson(
    @Expose @SerializedName("block_strengths") val blockStrengths: Map<String, Int>,

    @Expose @SerializedName("allowed_blocks") val categories: Map<String, List<NeuInternalName>>,
)
