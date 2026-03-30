package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration.Companion.seconds

@KSerializable
data class NeuForgeRecipeJson(
    @Expose override val type: NeuRecipeType = NeuRecipeType.FORGE,
    @Expose val inputs: List<NeuRecipeComponent>,
    @Expose val count: Int,
    @Expose val overrideOutputId: NeuInternalName? = null,
    @Expose @SerializedName("duration") private val durationSeconds: Int,
) : NeuAbstractRecipe {
    val duration by lazy { durationSeconds.seconds }
    override val primitiveIngredients by lazy {
        inputs.mapNotNull { it.toPrimitiveIngredientOrEmpty() }
    }
    override fun getPrimitiveOutputs(itemJson: NeuItemJson) = super.getPrimitiveOutputsFromJson(itemJson, count)
    override fun getPrimitiveRecipe(itemJson: NeuItemJson) = super.getBasePrimitiveRecipe(itemJson).withDuration(duration)
}
