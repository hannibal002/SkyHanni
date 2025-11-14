package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DianaJson(
    @Expose @SerializedName("spade_types") val spadeTypes: List<NeuInternalName>,
    @Expose @SerializedName("sphinx_questions") val sphinxQuestions: Map<String, String>,
    @Expose @SerializedName("mythological_mobs") val mythologicalCreatures: Map<String, MythologicalCreatureType>
)

@KSerializable
data class MythologicalCreatureType(
    @Expose val name: String,
    @Expose val rare: Boolean = false,
) {
    // If the class is annotated by @KSerializable, Gson directly uses the main data class constructor, meaning that
    // lazy fields get property initialized.
    val cleanName by lazy { name.removeColor() }
    val trackerId by lazy { cleanName.replace(" ", "_").uppercase() }
}
