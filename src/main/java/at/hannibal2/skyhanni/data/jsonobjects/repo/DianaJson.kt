package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.StringUtils.removeFormattingCodes
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DianaJson(
    @Expose @SerializedName("spade_types") val spadeTypes: List<NeuInternalName>,
    @Expose @SerializedName("sphinx_questions") val sphinxQuestions: Map<String, String>,
    @Expose @SerializedName("mythological_mobs") val mythologicalCreatures: Map<String, MythologicalCreatureType>
)

data class MythologicalCreatureType(
    @Expose val name: String,
    @Expose val rare: Boolean?
) {
    val cleanName
        get() = name.removeFormattingCodes()
    val trackerId
        get() = cleanName.replace(" ", "_").uppercase()
}
